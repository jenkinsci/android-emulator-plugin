package jenkins.plugin.android.emulator.sdk.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import jenkins.plugin.android.emulator.sdk.cli.Targets.TargetType;

public class AVDManagerCLIBuilderTest {

    @Test
    public void test_list_parse() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("avdmanager_list_target.out")) {
            List<Targets> targets = new AVDManagerCLIBuilder.ListTargetParser().parse(is);
            assertThat(targets).hasSize(1);
            Targets target = targets.get(0);
            assertThat(target.getId()).isEqualTo("android-21");
            assertThat(target.getName()).isEqualTo("Android API 21");
            assertThat(target.getType()).isEqualTo(TargetType.platform);
            assertThat(target.getApiLevel()).isEqualTo(21);
            assertThat(target.getRevision()).isEqualTo(2);
        }
    }

}
