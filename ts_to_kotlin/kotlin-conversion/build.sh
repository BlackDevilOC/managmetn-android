#!/bin/bash

# Create output directory
mkdir -p out

# Compile the models first
echo "Compiling Kotlin models..."
kotlinc src/main/kotlin/substitutemanager/models/Models.kt -d out/models.jar

# Compile the utils
echo "Compiling Kotlin utils..."
kotlinc -cp out/models.jar src/main/kotlin/substitutemanager/utils/CsvUtils.kt -d out/utils.jar

# Compile the SubstituteManager
echo "Compiling SubstituteManager..."
kotlinc -cp out/models.jar:out/utils.jar:libs/gson-2.10.1.jar src/main/kotlin/substitutemanager/SubstituteManager.kt -d out/substitute-manager.jar

# Compile the Main class and include everything in final jar
echo "Compiling Main..."
kotlinc -cp out/models.jar:out/utils.jar:out/substitute-manager.jar:libs/gson-2.10.1.jar:libs/kotlinx-coroutines-core-jvm-1.7.3.jar:libs/kotlin-stdlib-1.9.0.jar:libs/kotlin-stdlib-common-1.9.0.jar:libs/kotlin-stdlib-jdk7-1.9.0.jar:libs/kotlin-stdlib-jdk8-1.9.0.jar src/main/kotlin/substitutemanager/Main.kt -include-runtime -d out/app.jar

# Check if compilation was successful
if [ $? -eq 0 ]; then
  echo "Compilation successful!"
  echo "To run the application, use: java -jar out/app.jar"
else
  echo "Compilation failed."
fi
