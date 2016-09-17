package hudson.plugins.android_emulator.pipeline;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

@Extension
public class AndroidEmulatorDsl extends GlobalVariable {

    @Override
    public String getName() {
        return "androidEmulator";
    }

    @Override public Object getValue(CpsScript script) throws Exception {
        Binding binding = script.getBinding();
        Object impl;
        if (binding.hasVariable(getName())) {
            impl = binding.getVariable(getName());
        } else {
            // Note that if this were a method rather than a constructor,
            // we would need to mark it @NonCPS lest it throw CpsCallableInvocation.
            impl = script.getClass().getClassLoader()
                    .loadClass("hudson.plugins.android_emulator.pipeline.Dsl")
                    .getConstructor(CpsScript.class).newInstance(script);
            binding.setVariable(getName(), impl);
        }
        return impl;
    }

}
