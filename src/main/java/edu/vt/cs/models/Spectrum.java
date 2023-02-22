package edu.vt.cs.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableSpectrum.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Spectrum {

    @Value.Parameter
    Collection<Entity> getEntities();

    @Value.Parameter
    int getTotalOfPassedTests();

    @Value.Parameter
    int getTotalOfFailedTests();

    @Value.Parameter
    Project getProject();
}
