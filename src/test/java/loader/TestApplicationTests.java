package loader;

import com.alikian.localstack.loader.DockerLoaderClient;
import com.alikian.localstack.loader.DockerLoaderManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestApplicationTests {

    public TestApplicationTests() {
    }

    @Test
    public void contextLoads() {
        DockerLoaderClient dockerLoader = DockerLoaderClient.getInstance("/application-test.yml");
        DockerLoaderManager dockerLoaderConfig = dockerLoader.getDockerLoaderManager();
        dockerLoaderConfig.start();
        assertEquals(true, dockerLoaderConfig.getLocalstackDockerManager().isSuccess());
        assertEquals(true, dockerLoaderConfig.getMysqlDockerManager().isSuccess());
    }

}
