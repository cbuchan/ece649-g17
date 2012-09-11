SOURCE_DIR = code

.PHONY: doc

doc:
	rm -rf doc
	javadoc -overview $(SOURCE_DIR)/overview.html -package \
	    -sourcepath $(SOURCE_DIR) \
	    -source 1.5 -use -d $@ \
	    -doctitle "18-649 Elevator API" -windowtitle "18-649 API" \
	    simulator.framework simulator.elevatorcontrol \
	    simulator.elevatormodules simulator.payloads jSimPack \
	    simulator.framework.faultmodels simulator.framework.faults \
	    simulator.payloads.translators

# vi:noet
