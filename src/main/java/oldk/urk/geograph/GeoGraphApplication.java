package oldk.urk.geograph;

import com.fasterxml.classmate.TypeResolver;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ServiceLoader;

import static java.util.Collections.singletonList;

@SpringBootApplication
public class GeoGraphApplication {

	public static Logger LOGGER = LoggerFactory.getLogger(GeoGraphApplication.class);

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(GeoGraphApplication.class);
		if (!ObjectUtils.isEmpty(args))
			application.setWebApplicationType(WebApplicationType.NONE);
		else
			application.setAdditionalProfiles("web-service");
		application.run(args);
	}

	@Bean
	@ConfigurationProperties("spring.datasource.graph")
	public DataSourceProperties graphDataProps() {
		return new DataSourceProperties();
	}


	@Bean
	public DataSource graphData(DataSourceProperties graphDataProps) {
		try {
			Class.forName("org.neo4j.jdbc.bolt.BoltDriver");
		} catch (ClassNotFoundException x) {
			LOGGER.error("Internal error: failed init neo4j driver", x);
		}
		return graphDataProps.initializeDataSourceBuilder().build();
	}

	@Bean
	@ConfigurationProperties("spring.datasource.geo")
	public DataSourceProperties geoDataProps() {
		return new DataSourceProperties();
	}

	@Bean
	@Primary
	public HikariDataSource geoData(@Qualifier("geoDataProps") DataSourceProperties geoDataProps) {
		return geoDataProps.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}


	@Bean
	public SpringLiquibase liquibase(@Qualifier("graphData") DataSource graphDs)  {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDataSource(graphDs);
		liquibase.setChangeLog("classpath:/graph-db/change.xml");
		return liquibase;
	}


}
