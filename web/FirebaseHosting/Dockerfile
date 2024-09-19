ARG NODE_VERSION=latest

FROM node:${NODE_VERSION}-alpine

RUN apk update && apk add --no-cache \
    bash \
    nano

RUN npm install -g firebase-tools

WORKDIR /firebase

RUN mkdir -p /home/node/.config && chown node:node /home/node/.config

USER node
