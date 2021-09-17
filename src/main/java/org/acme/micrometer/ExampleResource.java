package org.acme.micrometer;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/example")
@Produces("text/plain")
public class ExampleResource {

    @ConfigProperty(name = "quarkus.micrometer.binder.http-server.ignore-patterns")
    String config;
    @ConfigProperty(name = "quarkus.micrometer.binder.http-client.enabled")
    boolean enabled;
    @ConfigProperty(name = "quarkus.micrometer.enabled")
    boolean enabled2;

    private final MeterRegistry registry;

    LinkedList<Long> list = new LinkedList<>();


    // Update the constructor to create the gauge
    ExampleResource(MeterRegistry registry) {
        /* Code for micrometer */
        StackdriverConfig stackdriverConfig = new StackdriverConfig() {
            @Override
            public String projectId() {
                return "fake-id";
            }

            @Override
            public String get(String key) {
                return null;
            }
        };
        this.registry = StackdriverMeterRegistry.builder(stackdriverConfig).build();
        this.registry.config().commonTags("application", "fake-id");
        new ClassLoaderMetrics().bindTo(this.registry);
        new JvmMemoryMetrics().bindTo(this.registry);
        new JvmGcMetrics().bindTo(this.registry);
        new ProcessorMetrics().bindTo(this.registry);
        new JvmThreadMetrics().bindTo(this.registry);

    }

    @GET
    @Path("gauge/{number}")
    public Long checkListSize(@PathParam("number") long number) {
        if (number == 2 || number % 2 == 0) {
            // add even numbers to the list
            list.add(number);
        } else {
            // remove items from the list for odd numbers
            try {
                number = list.removeFirst();
            } catch (NoSuchElementException nse) {
                number = 0;
            }
        }
        return number;
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
        registry.gauge("quarkus_example",25); // random gauge to test gauge

        example(); // buffer time to test timer

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

}


