package com.tilofy.test.imagecrawler;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.core.Application;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Injections;

import scala.concurrent.duration.Duration;
import akka.actor.ActorSystem;
import akka.routing.RoundRobinRouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ImageCrawlerApplication extends Application {

    // TODO: Must move JOB_ID_COUNTER and JOB_STATUS out of JVM if you want to
    // scale beyond 1 machine.

    // if this is truly be deployed in a "prod" env, it
    // needs to be centralized somewhere else: a database or zookeeper would be
    // nice. It should not live inside this JVM; otherwise, there's no
    // way of scaling to more ImangeCrawler servers since each will have it's
    // own jobIdCounter
    public static final AtomicInteger JOB_ID_COUNTER = new AtomicInteger(0);

    // creates a cache that evicts if the entry of age exceeds 10 mins
    public static final Cache<Integer, JobStatus> JOB_STATUS = CacheBuilder.newBuilder().maximumSize(10000)
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    public static class JobStatus {
        public BufferedImage image;
        public String message;
        public String extension;

        public JobStatus(BufferedImage image, String message, String extension) {
            this.image = image;
            this.message = message;
            this.extension = extension;
        }
    }

    private ActorSystem system;

    @Inject
    public ImageCrawlerApplication(ServiceLocator serviceLocator) {

        system = ActorSystem.create("ImageCrawlerSystem");

        // TODO: Adjust to increase parallel downloads/resizing jobs
        system.actorOf(ImageCrawlActor.mkProps().withRouter(new RoundRobinRouter(5)), "imagecrawlerrouter");

        DynamicConfiguration dc = Injections.getConfiguration(serviceLocator);
        Injections.addBinding(Injections.newBinder(system).to(ActorSystem.class), dc);
        dc.commit();
    }

    @PreDestroy
    private void shutdown() {
        system.shutdown();
        system.awaitTermination(Duration.create(15, TimeUnit.SECONDS));
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(ImageCrawlerService.class);
        return s;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> s = new HashSet<Object>();

        // Add this (w/ corresponding POM changes) to get "pretty printed" JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        s.add(new JacksonJsonProvider(mapper));

        return s;
    }
}