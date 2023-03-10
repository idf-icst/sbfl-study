package edu.vt.cs.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nonnull;
import java.util.List;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableBug.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Bug {

    @Value.Parameter
    Project getProject();

    @Value.Parameter
    int getBugId();

    @Value.Parameter
    List<String> getLocations();
}
