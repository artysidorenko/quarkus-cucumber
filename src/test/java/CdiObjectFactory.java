import java.nio.file.Path;
import java.nio.file.Paths;
import javax.enterprise.context.ApplicationScoped;
import io.cucumber.core.backend.ObjectFactory;
import io.quarkus.arc.Unremovable;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.test.common.PathTestHelper;

public class CdiObjectFactory implements ObjectFactory {
  private RunningQuarkusApplication application;

  public CdiObjectFactory() {
    startApplication();
  }

  public void start() {}

  public void stop() {}

  public boolean addClass(Class<?> stepClass) {
    if (stepClass.getAnnotation(ApplicationScoped.class) == null) {
      throw new RuntimeException(
        "You need to register your step definitions with @ApplicationScoped, otherwise Arc container does not register them as beans."
      );
    }
    if (stepClass.getAnnotation(Unremovable.class) == null) {
      throw new RuntimeException(
        "You need to register your step definitions with @Unremovable, otherwise Arc container may remove the beans at runtime"
      );
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  public <T> T getInstance(Class<T> type) {
    if (application == null) {
      throw new RuntimeException("Application has not been successfully started");
    }
    try {
      return (T) application.instance(type);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void startApplication() {
    try {
      PathsCollection.Builder rootBuilder = PathsCollection.builder();

      Path testClassLocation = PathTestHelper.getTestClassesLocation(CdiObjectFactory.class);

      // Load step definitions
      rootBuilder.add(testClassLocation);

      // Load application classes
      final Path appClassLocation = PathTestHelper.getAppClassLocationForTestLocation(testClassLocation.toString());
      rootBuilder.add(appClassLocation);

      QuarkusBootstrap.Builder applicationBuilder = QuarkusBootstrap.builder()
              .setIsolateDeployment(false)
              .setMode(QuarkusBootstrap.Mode.TEST)
              .setProjectRoot(Paths.get("").normalize().toAbsolutePath())
              .setApplicationRoot(rootBuilder.build());

      application = applicationBuilder
        .build()
        .bootstrap()
        .createAugmentor()
        .createInitialRuntimeApplication()
        .run();
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
