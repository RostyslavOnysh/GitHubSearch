### Building the Project

To build the project, execute the following command:

```bash
sh exec.sh build
```

This command will run the `scripts/build.sh` script, which cleans and packages the project using Maven and builds the Docker images.

### Running the Project

To run the project, use the following command:

```bash
sh exec.sh run
```

If the project has not been built previously, this command will automatically build it before starting the Docker containers.

### Accessing the Containers

- **Accessing the App Container:**

  To get shell access to the running `app` container, use:

  ```bash
  sh exec.sh bash:app
  ```

- **Accessing the Redis Container:**

  To get shell access to the running `redis` container, use:

  ```bash
  sh exec.sh bash:redis
  ```

### Viewing Logs

- **App Container Logs:**

  To view logs for the `app` container, run:

  ```bash
  sh exec.sh logs:app
  ```

- **Redis Container Logs:**

  To view logs for the `redis` container, run:

  ```bash
  sh exec.sh logs:redis
  ```

### Stopping the Application

To stop the application and all running containers, execute:

```bash
sh exec.sh stop
```

This command will stop all Docker containers related to the project using the `scripts/stop.sh` script.

### Clearing and Rebuilding the Project

If you need to clear all previous builds, images, and volumes, and then rebuild the project from scratch, use:

```bash
sh exec.sh clear
```

This will stop the containers, remove Docker images and volumes, and clean up the build directory before starting the build process again.

### Custom Docker Compose Commands

You can pass any Docker Compose command directly through the `exec.sh` script. For example:

- **View Logs:**

  ```bash
  sh exec.sh logs
  ```

- **Restart Containers:**

  ```bash
  sh exec.sh restart
  ```

## Script Overview

- **`exec.sh`:** The main script to manage the project's build, run, stop, and container access.
- **`scripts/build.sh`:** Cleans and packages the Maven project, then builds the Docker images.
- **`scripts/rebuild.sh`:** Clears previous builds and performs a clean build of the project.
- **`scripts/clear.sh`:** Removes Docker images, volumes, and previous builds, providing a clean slate.
- **`scripts/run.sh`:** Stops running containers, builds the project if necessary, and starts the containers.
- **`scripts/stop.sh`:** Stops the Docker containers associated with the project.

---