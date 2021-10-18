package jenkins.plugin.android.emulator.sdk.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.Test;

import jenkins.plugin.android.emulator.sdk.cli.SDKPackages.SDKPackage;

public class SDKManagerCLIBuilderTest {

    @Test
    public void test_list_parse() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("sdkmanager_list.out")) {
            SDKPackages packages = new SDKManagerCLIBuilder.ListPackagesParser().parse(is);
            assertThat(packages.getAvailable()).isNotEmpty().hasSize(236).allMatch(p -> p.getId() != null, "Name is null");
            assertThat(packages.getInstalled()).isNotEmpty().hasSize(6).allMatch(p -> p.getId() != null, "Name is null");
            assertThat(packages.getUpdates()).isNotEmpty().hasSize(1).allMatch(p -> p.getId() != null, "Name is null");
        }
    }

    @Test
    public void test_sort_packages() throws Exception {
        SDKPackage p1 = new SDKPackage();
        p1.setId("test");
        p1.setVersion(new Version("1.0.0 rc1"));

        SDKPackage p2 = new SDKPackage();
        p2.setId("test");
        p2.setVersion(new Version("1.0.0"));

        SDKPackage p3 = new SDKPackage();
        p3.setId("notest");
        p3.setVersion(new Version("1.0.0 rc1"));
        
        assertThat(p1).isLessThan(p2);
        assertThat(p1).isGreaterThan(p3);
    }
}
