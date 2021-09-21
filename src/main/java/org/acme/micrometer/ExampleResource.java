package org.acme.micrometer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.PathParam;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.micrometer.core.instrument.config.MeterFilter;

@Path("/")
public class ExampleResource {

    @ConfigProperty(name = "quarkus.micrometer.export.stackdriver.enabled")
    boolean enabled;
    @ConfigProperty(name = "quarkus.micrometer.export.stackdriver.default-registry")
    boolean export;
    @ConfigProperty(name="quarkus.micrometer.export.stackdriver.project-id")
    String projectId;
    @ConfigProperty(name="quarkus.micrometer.export.stackdriver.publish")
    boolean publish;
    @ConfigProperty(name="quarkus.micrometer.export.stackdriver.resource-type")
    String resourceType;
    @ConfigProperty(name="quarkus.micrometer.export.stackdriver.step")
    String step;

    private final MeterRegistry registry;

    ExampleResource(MeterRegistry reg) {
        
        this.registry = reg;
        new ClassLoaderMetrics().bindTo(this.registry);
        new JvmMemoryMetrics().bindTo(this.registry);
        new JvmGcMetrics().bindTo(this.registry);
        new ProcessorMetrics().bindTo(this.registry);
        new JvmThreadMetrics().bindTo(this.registry);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello RESTEasy";
    }

    @GET
    @Path("prime/{number}")
    @Timed
    @Counted
    public String checkIfPrime(@PathParam("number") long number) {
        Timer timer = registry.timer("example.prime.number.timer.milli");

        Timer.Sample sample = Timer.start(registry);


        if (number < 1) {
            registry.counter("example.prime.number", "type", "not-natural").increment();
            return "Only natural numbers can be prime numbers.";
        }
        if (number == 1) {
            registry.counter("example.prime.number", "type", "one").increment();
            return number + " is not prime.";
        }
        if (number == 2 || number % 2 == 0) {
            registry.counter("example.prime.number", "type", "even").increment();
            return number + " is not prime.";
        }

        timer.record(() -> testPrimeNumber(number));
        if (testPrimeNumber(number)) {
            registry.counter("example.prime.number", "type", "prime").increment();
            sample.stop(registry.timer("prime.timer"));
            return number + " is prime.";
        } else {
            registry.counter("example.prime.number", "typxe", "not-prime").increment();
            sample.stop(registry.timer("prime.timer"));

            return number + " is not prime.";
        }
    }

    protected boolean testPrimeNumber(long number) {
        Timer timer = registry.timer("example.test.prime.number.timer");
        registry.gauge("quarkus_example",25);

        System.out.println(registry.getMeters().toString());
        for (Meter meter : registry.getMeters()) {
            System.out.println(meter.getId() + " " + meter.toString());
        }

        example(); //random time

        return timer.record(() -> {
            for (int i = 3; i < Math.floor(Math.sqrt(number)) + 1; i = i + 2) {
                if (number % i == 0) {
                    return false;
                }
            }
            return true;
        });


    }

    @Timed
    public void example() {
        long start = System.currentTimeMillis();

        Timer timer = registry.timer("app.timer", "type", "ping");

        try {
            int i = 0;
            while (i < 3) {
                System.out.println("Running example use case: #" + i);

                Thread.sleep(1000);
                i++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        timer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    }


    @GET
    @Path("/print")
    public void printMeters() {
        System.out.println(this.registry.getMeters().toString());
        for (Meter meter : this.registry.getMeters()) {
            System.out.println(meter.getId() + " " + meter.toString());
        }
    }
}
