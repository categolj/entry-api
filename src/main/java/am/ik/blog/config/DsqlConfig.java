package am.ik.blog.config;

import com.zaxxer.hikari.SQLExceptionOverride;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

@Configuration(proxyBeanMethods = false)
@Profile("dsql")
class DsqlConfig {

	@Bean
	DsqlSQLExceptionTranslator dsqlSQLExceptionTranslator() {
		return new DsqlSQLExceptionTranslator();
	}

	@Bean
	JdbcTransactionManager transactionManager(DataSource dataSource,
			DsqlSQLExceptionTranslator dsqlSQLExceptionTranslator) {
		JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(dataSource);
		jdbcTransactionManager.setExceptionTranslator(dsqlSQLExceptionTranslator);
		return jdbcTransactionManager;
	}

	// https://catalog.workshops.aws/aurora-dsql/en-US/04-programming-with-aurora-dsql/02-handling-concurrency-conflicts
	private static final String DSQL_OPTIMISTIC_CONCURRENCY_ERROR_STATE = "40001";

	static class DsqlSQLExceptionTranslator implements SQLExceptionTranslator {

		SQLStateSQLExceptionTranslator delegate = new SQLStateSQLExceptionTranslator();

		@Override
		public DataAccessException translate(String task, String sql, SQLException ex) {
			if (DSQL_OPTIMISTIC_CONCURRENCY_ERROR_STATE.equals(ex.getSQLState())) {
				throw new OptimisticLockingFailureException(ex.getMessage(), ex);
			}
			return delegate.translate(task, sql, ex);
		}

	}

	public static class DsqlExceptionOverride implements SQLExceptionOverride {

		@java.lang.Override
		public Override adjudicate(SQLException ex) {
			if (DSQL_OPTIMISTIC_CONCURRENCY_ERROR_STATE.equals(ex.getSQLState())) {
				return Override.DO_NOT_EVICT;
			}
			return Override.CONTINUE_EVICT;
		}

	}

}