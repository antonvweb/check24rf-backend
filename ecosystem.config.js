module.exports = {
  apps: [
    {
      name: "check24rfAPI-dev",                     // имя процесса
      script: "java",                              // запускаем java
      args: "-jar target/check24rfAPI-1.0-SNAPSHOT.jar --spring.profiles.active=dev",  // параметры запуска
      cwd: "/var/www/check24rf/backend-dev/",              // рабочая директория, где лежит jar и target
      output: "/var/log/check24rfAPI-dev.log",     // stdout
      error: "/var/log/check24rfAPI-dev.err.log",  // stderr
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10,
      env: {
        JAVA_OPTS: "-Xms512m -Xmx1024m",           // пример JVM опций, можно добавить если нужно
        NODE_ENV: "production"
      }
    }
  ]
};
