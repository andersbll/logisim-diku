LOGISIM_JAR_FILE := logisim-generic-2.7.1.jar
JAR_FILE := logisim-diku.jar
BIN_DIR := ./bin

jar: classes
	jar cf $(JAR_FILE) MANIFEST.MF -C $(BIN_DIR) .

classes:
	mkdir -p $(BIN_DIR)
	find -L . -name "*.java" > srcfiles
	javac -nowarn -d $(BIN_DIR) -classpath $(LOGISIM_JAR_FILE) @srcfiles
	rm -f srcfiles

clean:
	rm -rf $(BIN_DIR)
	rm -f srcfiles

