/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.integration.jaxrs;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.ApplicationPath;

import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.integration.QuarkusPlatform;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.quarkus.runtime.services.resources.QuarkusWelcomeResource;
import org.keycloak.services.resources.WelcomeResource;

@ApplicationPath("/")
public class QuarkusKeycloakApplication extends KeycloakApplication {

    private static boolean filterSingletons(Object o) {
        return !WelcomeResource.class.isInstance(o);
    }

    @Inject
    Instance<EntityManagerFactory> entityManagerFactory;

    @Override
    protected void startup() {
        try {
            forceEntityManagerInitialization();
            initializeKeycloakSessionFactory();
            setupScheduledTasks(sessionFactory);
        } catch (Throwable cause) {
            QuarkusPlatform.exitOnError(cause);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = super.getSingletons().stream()
                .filter(QuarkusKeycloakApplication::filterSingletons)
                .collect(Collectors.toSet());

        singletons.add(new QuarkusWelcomeResource());
        singletons.add(new QuarkusWelcomeResource());

        return singletons;
    }

    private void initializeKeycloakSessionFactory() {
        QuarkusKeycloakSessionFactory instance = QuarkusKeycloakSessionFactory.getInstance();
        sessionFactory = instance;
        instance.init();
        sessionFactory.publish(new PostMigrationEvent());
    }

    private void forceEntityManagerInitialization() {
        // also forces an initialization of the entity manager so that providers don't need to wait for any initialization logic
        // when first creating an entity manager
        entityManagerFactory.get().createEntityManager().close();
    }
}