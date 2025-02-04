FROM meteor/meteor-base AS builder

USER mt
WORKDIR /home/mt

COPY --chown=mt . app/

WORKDIR /home/mt/app

RUN meteor npm install

RUN meteor build --directory target/ --server-only

FROM meteor/node:14.21.4-alpine3.17

WORKDIR /app

# Copy the built app from the previous stage and install production NPM packages
COPY --from=builder /home/mt/app/target /app
RUN cd /app/bundle/programs/server && npm install

# Expose the port the app runs on
EXPOSE 3000

# Define the command to run your app
CMD ["node", "/app/bundle/main.js"]
