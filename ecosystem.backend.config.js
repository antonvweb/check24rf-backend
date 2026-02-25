module.exports = {
  apps: [
    {
      name: "adminPanelService-dev",
      script: "java",
      args: "-Xms128m -Xmx384m -XX:+UseG1GC -jar /var/www/check/backend/adminPanelService/target/adminPanelService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check/backend",
      output: "/var/www/check/logs/adminPanelService-dev.log",
      error: "/var/www/check/logs/adminPanelService-dev.err.log",
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10
    },
    {
      name: "authService-dev",
      script: "java",
      args: "-Xms128m -Xmx384m -XX:+UseG1GC -jar /var/www/check/backend/authService/target/authService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check/backend",
      output: "/var/www/check/logs/authService-dev.log",
      error: "/var/www/check/logs/authService-dev.err.log",
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10
    },
    {
      name: "userService-dev",
      script: "java",
      args: "-Xms128m -Xmx384m -XX:+UseG1GC -jar /var/www/check/backend/userService/target/userService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check/backend",
      output: "/var/www/check/logs/userService-dev.log",
      error: "/var/www/check/logs/userService-dev.err.log",
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10
    },
    {
      name: "mcoService-dev",
      script: "java",
      args: "-Xms128m -Xmx384m -XX:+UseG1GC -jar /var/www/check/backend/mcoService/target/mcoService.jar --spring.profiles.active=dev",
      cwd: "/var/www/check/backend",
      output: "/var/www/check/logs/mcoService-dev.log",
      error: "/var/www/check/logs/mcoService-dev.err.log",
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_restarts: 10
    }
  ]
};