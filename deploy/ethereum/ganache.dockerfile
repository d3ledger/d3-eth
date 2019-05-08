FROM trufflesuite/ganache-cli:latest

# fix for alpine-node bug (https://github.com/nodejs/docker-node/issues/813#issuecomment-407339011)
RUN npm config set unsafe-perm true

RUN npm install -g ganache-cli

COPY ./docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]
