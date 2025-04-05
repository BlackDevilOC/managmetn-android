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

# Compile the Main class with runtime
echo "Compiling Main..."
kotlinc -cp out/models.jar:out/utils.jar:out/substitute-manager.jar:libs/gson-2.10.1.jar:libs/kotlinx-coroutines-core-jvm-1.7.3.jar:libs/kotlin-stdlib-1.9.0.jar:libs/kotlin-stdlib-common-1.9.0.jar:libs/kotlin-stdlib-jdk7-1.9.0.jar:libs/kotlin-stdlib-jdk8-1.9.0.jar src/main/kotlin/substitutemanager/Main.kt -include-runtime -d out/app.jar

# Create manifest file
mkdir -p META-INF
cat > META-INF/MANIFEST.MF << 'EOF'
Manifest-Version: 1.0
Main-Class: substitutemanager.MainKt
Class-Path: libs/gson-2.10.1.jar libs/kotlinx-coroutines-core-jvm-1.7.3.jar libs/kotlin-stdlib-1.9.0.jar libs/kotlin-stdlib-common-1.9.0.jar libs/kotlin-stdlib-jdk7-1.9.0.jar libs/kotlin-stdlib-jdk8-1.9.0.jar out/models.jar out/utils.jar out/substitute-manager.jar

EOF

# Extract and package all in one JAR
mkdir -p temp
cd temp
# Extract all jars
jar xf ../out/app.jar
jar xf ../out/models.jar
jar xf ../out/utils.jar
jar xf ../out/substitute-manager.jar
jar xf ../libs/gson-2.10.1.jar
jar xf ../libs/kotlinx-coroutines-core-jvm-1.7.3.jar
jar xf ../libs/kotlin-stdlib-1.9.0.jar
jar xf ../libs/kotlin-stdlib-common-1.9.0.jar
jar xf ../libs/kotlin-stdlib-jdk7-1.9.0.jar
jar xf ../libs/kotlin-stdlib-jdk8-1.9.0.jar
# Create the all-in-one JAR
cd ..
jar cfm out/substitute-manager-all-in-one.jar META-INF/MANIFEST.MF -C temp .

# Cleanup
rm -rf temp

# Check if all-in-one JAR creation was successful
if [ -f out/substitute-manager-all-in-one.jar ]; then
  echo "All-in-one JAR created successfully!"
  echo "To run the application, use: java -jar out/substitute-manager-all-in-one.jar"
else
  echo "JAR creation failed."
fi
