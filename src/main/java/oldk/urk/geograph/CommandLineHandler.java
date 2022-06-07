package oldk.urk.geograph;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
public class CommandLineHandler implements ApplicationRunner {
    Logger LOGGER = LoggerFactory.getLogger(CommandLineHandler.class);

    final DataSource graphDs;
    final DataSource geoDs;

    @Autowired
    public CommandLineHandler(@Qualifier("graphData") DataSource graphDs,
                              @Qualifier("geoData") DataSource geoDs) {
        this.graphDs = graphDs;
        this.geoDs = geoDs;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            if (!ObjectUtils.isEmpty(args.getSourceArgs())) {
                List<String> commands = args.getOptionValues("command");
                commands.forEach(cmd -> handleCommand(cmd, args));
            }
        } catch (IllegalArgumentException x) {
            LOGGER.error("Invalid command line args", x);
            showUsage();
        }
    }

    private void showUsage() {
        System.out.println("Usage: geoGraph --command=import --in-file=<file-name.pbf>");
    }

    private void handleCommand(String cmd, ApplicationArguments args) {
            if ("import".equals(cmd)) {
            if (!args.containsOption("in-file"))
                throw new IllegalArgumentException("in-file is not found!");
            List<String> fileNames = args.getOptionValues("in-file");
            if (fileNames.size() < 1)
                throw new IllegalArgumentException("in-file is empty");
            var executor = new ImportCommandExecutor(fileNames.get(0), graphDs, geoDs);
            executor.execute();
        }
    }
}
