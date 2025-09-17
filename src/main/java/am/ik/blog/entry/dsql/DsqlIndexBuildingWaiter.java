package am.ik.blog.entry.dsql;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("dsql")
public class DsqlIndexBuildingWaiter implements CommandLineRunner {

	private final JdbcClient jdbcClient;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public DsqlIndexBuildingWaiter(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public void run(String... args) throws Exception {
		for (int i = 0; i < 20; i++) {
			List<String> statuses = this.jdbcClient.sql("""
					SELECT
					    object_name,
					    MAX(start_time) as latest_start_time,
					    (SELECT status
					     FROM sys.jobs j2
					     WHERE j2.object_name = j1.object_name
					       AND j2.start_time = MAX(j1.start_time)
					       AND j2.job_type = 'INDEX_BUILD'
					     LIMIT 1) as status
					FROM sys.jobs j1
					WHERE object_name IN (
					    'public.entry_unique_key',
					    'public.entry_last_modified_date',
					    'public.entry_categories_entry_id_idx',
					    'public.entry_categories_name_idx',
					    'public.entry_categories_unique_key',
					    'public.entry_tags_entry_id_idx',
					    'public.entry_tags_name_idx',
					    'public.entry_tokens_token_idx'
					)
					AND job_type = 'INDEX_BUILD'
					GROUP BY object_name
					ORDER BY object_name
					""").query((rs, __) -> rs.getString("status")).list();
			Set<String> statusSet = new HashSet<>(statuses);
			if (statusSet.isEmpty()) {
				logger.info("No index building jobs found.");
				break;
			}
			else if (statusSet.size() == 1 && statusSet.contains("completed")) {
				logger.info("Index building completed.");
				break;
			}
			else {
				logger.info("Index building in progress: {}", statuses);
				Thread.sleep(3_000L);
			}
		}
	}

}
