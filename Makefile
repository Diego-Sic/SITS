SHELL := /bin/bash
MVN   ?= mvn

SERVER       ?= 192.168.1.42
PORT         ?= 8080
TOURNAMENT   ?= ipd-01
CLIENT1_NAME ?= TitForTat
CLIENT2_NAME ?= AlwaysDefect

# empty until the ClientApp host-override patch lands; then set to Machine B's LAN IP
PARTICIPANT_HOST ?=

SERVER_URL := http://$(SERVER):$(PORT)

.PHONY: help ip open-port close-port kill-port check \
        server client1 client2 clients viewer \
        build test clean

help:
	@echo "Targets: server | client1 | client2 | viewer | check | ip | open-port | close-port | kill-port | build | test | clean"
	@echo "Overrides: SERVER PORT TOURNAMENT CLIENT1_NAME CLIENT2_NAME PARTICIPANT_HOST"

ip:
	@hostname -I | awk '{print $$1}'   # $$1 escapes $ so awk sees $1, not a Make variable

open-port:
	sudo ufw allow $(PORT)/tcp

close-port:
	sudo ufw delete allow $(PORT)/tcp

kill-port:
	-lsof -ti:$(PORT) | xargs -r kill -9   # leading dash: don't fail the target if no process holds the port

server:
	$(MVN) spring-boot:run \
	  -Dspring-boot.run.mainClass=sits.server.TournamentServerApp

# spring-boot expects a comma-separated arg list; building it in shell lets us conditionally append --participant.host
# I am still studying this and why it works, but it seems to be a common pattern for passing complex args to make targets
define run_client
	args="--tournament.server.url=$(SERVER_URL),--tournament.id=$(TOURNAMENT),--server.port=0,--participant.name=$(1)"; \
	if [ -n "$(PARTICIPANT_HOST)" ]; then \
	  args="$$args,--participant.host=$(PARTICIPANT_HOST)"; \
	fi; \
	$(MVN) spring-boot:run \
	  -Dspring-boot.run.mainClass=sits.client.ClientApp \
	  -Dspring-boot.run.arguments="$$args"
endef

client1:
	@$(call run_client,$(CLIENT1_NAME))

client2:
	@$(call run_client,$(CLIENT2_NAME))

# backgrounds client2 so both run from one shell; prefer two terminals for a live demo so logs stay readable
clients:
	@$(MAKE) client2 &
	@$(MAKE) client1

viewer:
	$(MVN) javafx:run

check:
	@curl -fsS $(SERVER_URL)/tournaments | head -c 400; echo

build:
	$(MVN) -DskipTests package

test:
	$(MVN) test

clean:
	$(MVN) clean
