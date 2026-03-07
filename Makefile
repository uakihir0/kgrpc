build:
	./gradlew core:clean core:assemble -x check --refresh-dependencies

pods:
	./gradlew all:assembleKgrpcXCFramework all:podPublishXCFramework -x check --refresh-dependencies

native:
	cd native && bash compile.sh

version:
	./gradlew version --no-daemon --console=plain -q

.PHONY: build pods native version
