FROM trufflesuite/ganache-cli:latest
RUN npm install -g ganache-cli
COPY ./docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]
