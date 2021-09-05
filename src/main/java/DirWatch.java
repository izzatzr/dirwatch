import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class DirWatch implements QuarkusApplication  {

	private static Map<WatchKey, Path> watchKeyToPathMap = new HashMap<>();
	
	private static void watch(WatchService watchService, Path start) throws IOException, InterruptedException {
		registerTree(watchService, start);
    while (true) {
      WatchKey key = watchService.take();
      for (WatchEvent<?> watchEvent : key.pollEvents()) {
        Kind<?> kind = watchEvent.kind();
        Path eventPath = (Path) watchEvent.context();

        // Path directory = watchKeyToPathMap.get(key);
        Path directory = (Path) key.watchable(); //problems with renames 
        
        Path child = directory.resolve(eventPath);

        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(child)) {
          registerTree(watchService, child);
        }

        System.out.println(String.format("%s: %s", child, kind));
      }

      boolean valid = key.reset();
      if (!valid) {
        watchKeyToPathMap.remove(key);
        if (watchKeyToPathMap.isEmpty()) {
          break;
        }
      }
    }
  }

	private static void registerTree(WatchService watchService, Path start) throws IOException {
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        watchKeyToPathMap.put(key, dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

	@Override
	public int run(String... args) throws Exception {
		WatchService watchService
          = FileSystems.getDefault().newWatchService();
		watch(watchService, Paths.get(System.getProperty("user.dir")));
		return 0;
	}
}