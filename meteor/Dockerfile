FROM meteor/meteor-base

USER mt
WORKDIR /home/mt

COPY --chown=mt . app/

WORKDIR /home/mt/app

RUN meteor build --directory target/ --server-only
