# Use Java 25 JDK (Amazon Corretto)


# Set working directory inside container
WORKDIR /app

# Copy project files
COPY lib lib
COPY src src

# Compile Java files and create runnable JAR inside container
RUN mkdir bin \
    && javac -d bin -cp "lib/*" src/model/*.java src/syncControl/*.java src/Main.java \
    && cp -r src/logging bin/logging \
    && cd bin \
    && jar cfe JavaCinema.jar Main *.class model/*.class syncControl/*.class logging/*

# Set entrypoint; arguments can be passed at runtime
ENTRYPOINT ["java", "-cp", "bin/JavaCinema.jar:lib/*", "Main"]
