package net.lehir.denelezh;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.HashSet;
import java.util.Map;


import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.wikidata.wdtk.dumpfiles.DumpContentType;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.dumpfiles.MwDumpFile;
import org.wikidata.wdtk.dumpfiles.MwLocalDumpFile;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class);

    private static final String DUMP_DIRECTORY = System.getenv("HUMANIKI_DUMP_DIR");
    private static String TMP_DIRECTORY = System.getenv("HUMANIKI_WDTK_OUT_DIR");

    public static void configureLogging() {
        ConsoleAppender consoleAppender = new ConsoleAppender();
        String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p - %m%n";
        consoleAppender.setLayout(new PatternLayout(pattern));
        consoleAppender.setThreshold(Level.DEBUG);
        consoleAppender.activateOptions();
        Logger.getRootLogger().addAppender(consoleAppender);
    }

    public static void main(String[] args) {

        configureLogging();

        LOGGER.info("Starting...");

        System.setProperty("user.dir", DUMP_DIRECTORY);
        DumpProcessingController dumpProcessingController = new DumpProcessingController("wikidatawiki");

        MwDumpFile dumpFile = null;
        if (args.length == 0) {
            dumpProcessingController.setOfflineMode(false);
            dumpFile = dumpProcessingController.getMostRecentDump(DumpContentType.JSON);
            TMP_DIRECTORY = TMP_DIRECTORY + "latest/";
        } else if (args.length == 1) {
            String date = args[0];
            dumpProcessingController.setOfflineMode(false);
            // this checking if the final dir endswith wikidata/ is not a great method, but in use for now to distinguish between dev and prod
            String dumpDatePath = DUMP_DIRECTORY.endsWith("wikidata/") ? DUMP_DIRECTORY + date + ".json.gz" : DUMP_DIRECTORY + "dumpfiles/wikidatawiki/json-" + date + "/" + date + ".json.gz";
            dumpFile = new MwLocalDumpFile(dumpDatePath, DumpContentType.JSON, date, "wikidatawiki");
            TMP_DIRECTORY = TMP_DIRECTORY + date + "/";

            String envMaxHumans = System.getenv("HUMANIKI_MAX_HUMANS");
            Integer maxHumansToProcess = envMaxHumans != null ? Integer.parseInt(envMaxHumans) : null;
            if (maxHumansToProcess != null) {
                TMP_DIRECTORY = TMP_DIRECTORY + maxHumansToProcess + "/";
            }
            File directory = new File(TMP_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }

        } else {
            System.out.println("Invalid number of arguments.");
        }

        if (dumpFile != null) {

            try (FileWriter dumpFW = new FileWriter(TMP_DIRECTORY + "dump.csv");
                 BufferedWriter dumpBW = new BufferedWriter(dumpFW);
                 FileWriter humanFW = new FileWriter(TMP_DIRECTORY + "human.csv");
                 BufferedWriter humanBW = new BufferedWriter(humanFW);
                 FileWriter humanCountryFW = new FileWriter(TMP_DIRECTORY + "human_country.csv");
                 BufferedWriter humanCountryBW = new BufferedWriter(humanCountryFW);
                 FileWriter humanOccupationFW = new FileWriter(TMP_DIRECTORY + "human_occupation.csv");
                 BufferedWriter humanOccupationBW = new BufferedWriter(humanOccupationFW);
                 FileWriter humanSiteLinkFW = new FileWriter(TMP_DIRECTORY + "human_sitelink.csv");
                 BufferedWriter humanSiteLinkBW = new BufferedWriter(humanSiteLinkFW);
                 FileWriter labelFW = new FileWriter(TMP_DIRECTORY + "label.csv");
                 BufferedWriter labelBW = new BufferedWriter(labelFW);
                 FileWriter occupationFileWriter = new FileWriter(TMP_DIRECTORY + "occupation.csv");
                 BufferedWriter occupationBufferedWriter = new BufferedWriter(occupationFileWriter);
                 FileWriter occupation_parentFileWriter = new FileWriter(TMP_DIRECTORY + "occupation_parent.csv");
                 BufferedWriter occupation_parentBufferedWriter = new BufferedWriter(occupation_parentFileWriter)) {

                String dumpDateStamp = dumpFile.getDateStamp();
                dumpDateStamp = dumpDateStamp.substring(0, 4) + "-" + dumpDateStamp.substring(4, 6) + "-" + dumpDateStamp.substring(6, 8);

                dumpFW.write(dumpDateStamp);

                HumanProcessor gapsProcessor = new HumanProcessor(humanBW, humanCountryBW, humanOccupationBW, humanSiteLinkBW, labelBW);
                dumpProcessingController.registerEntityDocumentProcessor(gapsProcessor, null, true);

                dumpProcessingController.processDump(dumpFile);

                LOGGER.info("Occupations...");

                Map<Long, Occupation> occupations = Occupation.getOccupations();
                for (Long occupationId : occupations.keySet()) {
                    Occupation occupation = occupations.get(occupationId);
                    if (occupation.isTrueOccupation) {
                        occupationBufferedWriter.write(occupationId + "\n");
                        HashSet<Long> allParents = new HashSet<>();
                        occupation.getAllParents(allParents);
                        for (Long parentId : allParents) {
                            if (parentId != occupationId) {
                                Occupation parent = Occupation.getOccupation(parentId);
                                if (parent.isTrueOccupation) {
                                    occupation_parentBufferedWriter.write(occupationId + "," + parentId + "\n");
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.error(e);
                System.exit(1);
            }

        }

        LOGGER.info("Finished.");

    }

}
