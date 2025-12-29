module.exports = {
  apps: [
    {
      name: "mcoService-dev",
      script: "java",
      args: "-jar mcoService/target/mcoService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check/backend/",
      output: "/var/www/check/logs/mcoService-dev.log",
      error: "/var/www/check/logs/mcoService-dev.err.log",
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
      name: "adminPanelService-dev",
      script: "java",
      args: "-jar adminPanelService/target/adminPanelService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check/backend/",
      output: "/var/www/check/logs/adminPanelService-dev.log",
      error: "/var/www/check/logs/adminPanelService-dev.err.log",
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
      cwd: "/var/www/check/backend/",
      output: "/var/www/check/logs/authService-dev.log",
      error: "/var/www/check/logs/authService-dev.err.log",
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
      cwd: "/var/www/check/backend/",
      output: "/var/www/check/logs/userService-dev.log",
      error: "/var/www/check/logs/userService-dev.err.log",
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
      name: "billingService-dev",
      script: "java",
      args: "-jar billingService/target/billingService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check/backend/",
      output: "/var/www/check/logs/billingService-dev.log",
      error: "/var/www/check/logs/billingService-dev.err.log",
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