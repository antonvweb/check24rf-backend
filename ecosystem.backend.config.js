module.exports = {
  apps: [
    {
      name: "adminPanelService-dev",
      script: "java",
      args: "-jar adminPanelService/target/adminPanelService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check24rf/backend-dev/",
      output: "/var/log/adminPanelService-dev.log",
      error: "/var/log/adminPanelService-dev.err.log",
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10,
      env: {
        JAVA_OPTS: "-Xms512m -Xmx1024m",
        NODE_ENV: "production"
      }
    },
    {
      name: "authService-dev",
      script: "java",
      args: "-jar authService/target/authService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check24rf/backend-dev/",
      output: "/var/log/authService-dev.log",
      error: "/var/log/authService-dev.err.log",
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10,
      env: {
        JAVA_OPTS: "-Xms512m -Xmx1024m",
        NODE_ENV: "production"
      }
     },
     {
      name: "userService-dev",
      script: "java",
      args: "-jar userService/target/userService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check24rf/backend-dev/",
      output: "/var/log/userService-dev.log",
      error: "/var/log/userService-dev.err.log",
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10,
      env: {
        JAVA_OPTS: "-Xms512m -Xmx1024m",
        NODE_ENV: "production"
      }
     }
  ]
};