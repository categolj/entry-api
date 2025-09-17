package am.ik.blog.entry.dsql;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntryMaintenanceController {

	private final DsqlEntryRepository dsqlEntryRepository;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public EntryMaintenanceController(DsqlEntryRepository dsqlEntryRepository) {
		this.dsqlEntryRepository = dsqlEntryRepository;
	}

	@DeleteMapping(path = "/entries/{entryId}/tokens")
	public void deleteTokensForEntryId(@PathVariable UUID entryId) {
		log.info("Deleting tokens for entryId {}", entryId);
		this.dsqlEntryRepository.deleteTokens(entryId);
	}

}
