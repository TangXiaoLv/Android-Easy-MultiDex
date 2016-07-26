package com.ceabie.dexknife;

import org.gradle.api.tasks.util.PatternSet;

import java.util.Set;

/**
 * The type Dex knife config.
 *
 * @author ceabie
 */
public class DexKnifeConfig {
    public PatternSet patternSet;
    public boolean useSuggest = true;
    public boolean logMainList = false;
    public Set<String> additionalParameters;
}
