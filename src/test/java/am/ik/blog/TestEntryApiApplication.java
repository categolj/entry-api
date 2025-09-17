package am.ik.blog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.SpringApplication;

public class TestEntryApiApplication {

	public static void main(String[] args) {
		List<String> newArgs = new ArrayList<>(Arrays.asList(args));
		newArgs.add("--spring.docker.compose.enabled=false");
		SpringApplication.from(EntryApiApplication::main)
			.with(TestcontainersConfiguration.class)
			.run(newArgs.toArray(String[]::new));
	}

}
