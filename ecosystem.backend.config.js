module.exports = {
  apps: [
  {
        name: "mcoService-dev",
        script: "java",
        args: "-jar mcoService/target/mcoService.jar --spring.profiles.active=dev",
        cwd: "/var/www/check24rf/backend-dev/",
        output: "/var/log/mcoService-dev.log",
        error: "/var/log/mcoService-dev.err.log",
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
     },
     {
      name: "billingService-dev",
      script: "java",
      args: "-jar billingService/target/billingService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check24rf/backend-dev/",
      output: "/var/log/billingService-dev.log",
      error: "/var/log/billingService-dev.err.log",
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