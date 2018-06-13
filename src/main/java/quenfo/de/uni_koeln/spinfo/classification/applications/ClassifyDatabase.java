package quenfo.de.uni_koeln.spinfo.classification.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;
import quenfo.de.uni_koeln.spinfo.classification.jasc.workflow.ConfigurableDatabaseClassifier;

/**
 * 
 * @author geduldia
 * 
 *         Workflow zur Segmentierung von Stellenanzeigen in Abschnitte und
 *         Klassifikation der Abschnitte in die vier Kategorien:
 * 
 *         - Unternehmensbeschreibung - Jobbeschreibung - Bewerberprofil -
 *         Sonstiges/Formalia
 * 
 *         Im Konsolen-Dialog kann entweder ein bereits gespeichertes
 *         Klassifikationsmodel ausgewählt werden, oder ein neues erzeugt
 *         werden. Es werden zunächst nur die ersten [fetchSize] Stellenanzeigen
 *         verarbeitet. Danach kann ausgewählt werden, ob man
 *         das Programm nochmal stoppen (s), die nächsten [fetchSize]
 *         Stellenanzeigen verarbeiten, oder ohne Unterbrechung zu Ende
 *         klassifizieren lässt.
 *
 */
public class ClassifyDatabase {

	// Pfad zur Input-DB
	static String inputDB = "src/main/resources/classification/input/JobAds.db";

	// Name des Input-Tables
	static String inputTable = "JobAds";

	// Pfad zum Output-Ordner in dem die neue DB angelegt werden soll
	static String outputFolder = "src/main/resources/classification/output/";

	// Name der  Output-DB (Input für alle späteren
	// IE-Applications )
	static String outputDB = "ClassifiedParagraphs.db";

	// Pfad zur Datei mit den Trainingsdaten
	static String trainingdataFile = "src/main/resources/classification/input/trainingdata/trainingdata_anonymized.tsv";

	// Anzahl der Stellenanzeigen aus der Inpput-DB, die klassifiziert werden sollen (-1 = gesamte
	// Tabelle)
	static int queryLimit = -1;

	// falls nur eine begrenzte Anzahl von Anzeigen klassifiziert werden soll
	// (s.o.): hier die Startposition angeben
	static int startId = 0;

	// Die Anzeigen werden nicht alle auf einmal ausgelesen,
	// sondern Päckchenweise - hier angeben, wieviele jeweils in einem Schwung
	// zusammen verarbeitet werden sollen
	// nach dem ersten Schwung erscheint in der Konsole ein Dialog, in dem man
	// das Programm nochmal stoppen (s), die nächsten [fetchSize] Anzeigen
	// klassifizieren
	// (c), oder ohne Unterbrechung zu Ende klassifizieren lassen kann (d)
	static int fetchSize = 100;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		// Connect to input database
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out.println(
					"Database '" + inputDB + "' does not exists \nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = Class_DBConnector.connect(inputDB);
		}

		// Connect to output database
		Connection outputConnection = null;
		File outputDBFile = new File(outputFolder + outputDB);

		// if outputDB already exists
		if (outputDBFile.exists()) {
			outputConnection = Class_DBConnector.connect(outputFolder + outputDB);
			// use or override current outputDatabase
			System.out.println("\noutput-database " + outputDB + " already exists. "
					+ "\n - press 'o' to overwrite it (deletes all prior entries - annotated Trainingsparagraphs are saved in Trainingdatabase)"
					+ "\n - press 'u' to use it (adds and replaces entries)"
					+ "\n - press 'c' to create a new Output-Database");
			boolean answered = false;
			while (!answered) {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				String answer = in.readLine();
				if (answer.toLowerCase().trim().equals("o")) {
					outputConnection = Class_DBConnector.connect(outputFolder + outputDB);
					Class_DBConnector.createClassificationOutputTables(outputConnection);
					answered = true;
				} else if (answer.toLowerCase().trim().equals("u")) {
					outputConnection = Class_DBConnector.connect(outputFolder + outputDB);
					answered = true;
				} else if (answer.toLowerCase().trim().equals("c")) {
					System.out.println("Please enter the name of the new correctable Database. It will be stored in "
							+ outputFolder);
					in = new BufferedReader(new InputStreamReader(System.in));
					outputDB = in.readLine();
					outputConnection = Class_DBConnector.connect(outputFolder + outputDB);
					Class_DBConnector.createClassificationOutputTables(outputConnection);
					System.out.println(
							"Please enter the name of the new original Database. It will be stored in " + outputFolder);
					in = new BufferedReader(new InputStreamReader(System.in));
					answered = true;
				} else {
					System.out.println(" invalid answer! please try again...");
					System.out.println();
				}
			}
		}
		// if output database does not exist
		else {
			// create output-directory if not exists
			if (!new File(outputFolder).exists()) {
				new File(outputFolder).mkdirs();
			}
			// create output-database
			outputConnection = Class_DBConnector.connect(outputFolder + outputDB);
			Class_DBConnector.createClassificationOutputTables(outputConnection);
		}
		// start classifying
		long before = System.currentTimeMillis();

		ConfigurableDatabaseClassifier dbClassfy = new ConfigurableDatabaseClassifier(inputConnection, outputConnection,
				queryLimit, fetchSize, startId, trainingdataFile);
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("\nstart classification....\n\n");
			sb.append("\ninputDB: " + inputDB.substring(inputDB.lastIndexOf("/") + 1));
			sb.append("\noutputDB: " + outputDBFile.getName());
			sb.append("\nused Trainingdata: ");
			sb.append(trainingdataFile.substring(trainingdataFile.lastIndexOf("/") + 1));
			sb.append("\n\n");
			dbClassfy.classify(sb, inputTable);
		} catch (Exception e) {
			e.printStackTrace();
		}

		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60) {
			System.out.println("\nfinished Classification in " + (time / 60) + "hours");
		} else {
			System.out.println("\nfinished Classification in " + time + " minutes");
		}
	}

}
