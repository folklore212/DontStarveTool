mod_mod_replayguard.la: mod_mod_replayguard.slo
	$(SH_LINK) -rpath $(libexecdir) -module -avoid-version  mod_mod_replayguard.lo
DISTCLEAN_TARGETS = modules.mk
shared =  mod_mod_replayguard.la
