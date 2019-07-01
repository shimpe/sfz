SfzReader {
    classvar <regex_ws = "[ \t\r\n]+";
    classvar <regex_comment = "//[^\r\n]*\r?\n";

    var <>id;
    var <>path;
    var <>filecontents;
    var <>control;
    var <>sfzdata;
    var <>synths;
    var <>buffers;

    *new {
        | id = "rdr" |
        ^super.new.init(id);
    }

    init {
        | id |
        this.id = id;
        this.path = "";
        this.filecontents = "";
        this.sfzdata = ();
        this.synths = [];
        this.buffers = ();
    }

    freeMemory {
        this.synths.do({ | synth | synth.free; });
        this.synths = [];
        this.buffers.do({ | buffer | buffer.free; });
        this.buffers = [];
    }

    load {
        | server, path |
        ("load file" + path).postln;
        this.pr_loadFile(path);
        ("parse contents").postln;
        this.pr_parseContents;
        ("create synthdefs").postln;
        this.pr_createSynthDefs;
        ("load buffers").postln;
        this.pr_loadBuffers(server);
        server.sync;
        this.buffers;
        ^this.filecontents;
    }

    play {
        | out=0, freq=440, amp=0.5, dur=1, legato=0.9 |
        var noteregions = this.pr_getActiveRegions(freq, amp);
        noteregions.do({
            | el |
            var refnote = el[0];
            var region = el[1];
            var keyelements = el[2];
            var masterid = keyelements[0];
            var groupid = keyelements[1];
            var regionid = keyelements[2];
            var buf = this.buffers[region];

            var pan = this.getProperty(masterid, groupid, regionid, \pan, 0).asInteger/100.0;
            var xf=1;
            var ampeg_start = this.getProperty(masterid, groupid, regionid, \ampeg_start, 0).asFloat;
            var ampeg_delay = this.getProperty(masterid, groupid, regionid, \ampeg_delay, 0).asFloat;
            var ampeg_attack = this.getProperty(masterid, groupid, regionid, \ampeg_attack, 0.001).asFloat;
            var ampeg_sustain = this.getProperty(masterid, groupid, regionid, \ampeg_sustain, 1).asFloat;
            var ampeg_decay = this.getProperty(masterid, groupid, regionid, \ampeg_decay, 0.01).asFloat;
            var ampeg_release = this.getProperty(masterid, groupid, regionid, \ampeg_release, 0.01).asFloat;
            var ampeg_vel2delay = this.getProperty(masterid, groupid, regionid, \ampeg_vel2delay, 0).asFloat;
            var ampeg_vel2attack = this.getProperty(masterid, groupid, regionid, \ampeg_vel2attack, 0).asFloat;
            var ampeg_vel2sustain = this.getProperty(masterid, groupid, regionid, \ampeg_vel2sustain, 0).asFloat;
            var ampeg_vel2decay = this.getProperty(masterid, groupid, regionid, \ampeg_vel2decay, 0).asFloat;
            var ampeg_vel2release = this.getProperty(masterid, groupid, regionid, \ampeg_vel2release, 0).asFloat;
            var ampeg_vel2hold = this.getProperty(masterid, groupid, regionid, \ampeg_vel2hold, 0).asFloat;
            var volume = this.getProperty(masterid, groupid, regionid,\volume,0).asFloat;

            var chan = buf.numChannels;

            var synth;
            var playOnRelease = this.getProperty(masterid, groupid, regionid, \trigger, "attack").compare("release") == 0;

            if (playOnRelease.not) {
                //"start one-shot".postln;
                synth = Synth((this.id ++ "playbuf" ++ chan.clip(1,2)),
                    [\out, out,
                        \bufnum, buf.bufnum,
                        \amp, amp,
                        \volume, volume,
                        \xf, this.pr_calcXf(masterid, groupid, regionid, freq, amp),
                        \gate, 1,
                        \pan, pan,
                        \ampeg_hold, (dur*legato).debug("dur*legato"),
                        \rate, ((freq.cpsmidi)-(refnote.asInteger)).midiratio,
                        \ampeg_start, ampeg_start,
                        \ampeg_delay, ampeg_delay,
                        \ampeg_attack, ampeg_attack,
                        \ampeg_sustain, ampeg_sustain,
                        \ampeg_decay, ampeg_decay,
                        \ampeg_release, ampeg_release,
                        \ampeg_vel2delay, ampeg_vel2delay,
                        \ampeg_vel2attack, ampeg_vel2attack,
                        \ampeg_vel2sustain, ampeg_vel2sustain,
                        \ampeg_vel2decay, ampeg_vel2decay,
                        \ampeg_vel2release, ampeg_vel2release,
                        \ampeg_vel2hold, ampeg_vel2hold]);
            };

            TempoClock.sched((dur*legato), {
                if (playOnRelease.not) {
                    //"stop one-shot".postln;
                    synth.set(\gate, 0);
                    nil;
                } {
                    //"play on release".postln;
                    synth = Synth((this.id ++ "playbuf" ++ chan.clip(1,2)),
                        [\out, out,
                            \bufnum, buf.bufnum,
                            \amp, amp,
                            \volume, volume,
                            \xf, this.pr_calcXf(masterid, groupid, regionid, freq, amp),
                            \gate, 1,
                            \pan, pan,
                            \ampeg_hold, (dur*legato),
                            \rate, ((freq.cpsmidi)-(refnote.asInteger)).midiratio,
                            \ampeg_start, ampeg_start,
                            \ampeg_delay, ampeg_delay,
                            \ampeg_attack, ampeg_attack,
                            \ampeg_sustain, ampeg_sustain,
                            \ampeg_decay, ampeg_decay,
                            \ampeg_release, ampeg_release,
                            \ampeg_vel2delay, ampeg_vel2delay,
                            \ampeg_vel2attack, ampeg_vel2attack,
                            \ampeg_vel2sustain, ampeg_vel2sustain,
                            \ampeg_vel2decay, ampeg_vel2decay,
                            \ampeg_vel2release, ampeg_vel2release,
                            \ampeg_vel2hold, ampeg_vel2hold]);

                    TempoClock.sched((buf.numFrames / buf.sampleRate), {
                        //"stop on release".postln;
                        synth.set(\gate, 0); nil; } );
                };
            });
        });
    }

    getProperty {
        | masterid, groupid, regionid, property, defaultifmissing=nil, maybenote=false |
        var returnvalue = defaultifmissing;
        masterid = masterid.asSymbol;
        groupid = groupid.asSymbol;
        regionid= regionid.asSymbol;
        if (this.sfzdata[\0].isNil) {
            ^defaultifmissing;
        };
        if (this.sfzdata[\0][masterid].isNil) {
            masterid = \0;
        };
        if (this.sfzdata[\0][masterid].isNil) {
            ^defaultifmissing;
        };
        if (this.sfzdata[\0][masterid][groupid].isNil) {
            groupid = \0;
        };
        if (this.sfzdata[\0][masterid][groupid].isNil) {
            ^defaultifmissing;
        };
        if (this.sfzdata[\0][masterid][groupid][regionid].isNil) {
            regionid = \0;
        };
        if (this.sfzdata[\0][masterid][groupid][regionid].isNil) {
            ^defaultifmissing;
        };
        returnvalue = defaultifmissing;
        if (this.sfzdata[\0][masterid][groupid][regionid][property].isNil) {
            if (this.sfzdata[\0][masterid][groupid][\0][property].isNil) {
                ^defaultifmissing;
            } {
                returnvalue = this.sfzdata[\0][masterid][groupid][\0][property];
            };
        } {
            returnvalue = this.sfzdata[\0][masterid][groupid][regionid][property];
        };
        if (maybenote) {
            var theorynote = TheoryNoteParser();
            if (theorynote.asMidi(returnvalue)[0].notNil) {
                returnvalue = theorynote.asMidi(returnvalue)[0];
            }
        };
        ^returnvalue;
    }

    listProperties {
        | masterid, groupid, regionid |
        var result, result2, result3;
        masterid = masterid.asSymbol;
        groupid = groupid.asSymbol;
        regionid = regionid.asSymbol;
        result = this.sfzdata[\0][masterid][\0][\0];
        result2 = this.sfzdata[\0][masterid][groupid][\0];
        result3 = this.sfzdata[\0][masterid][groupid][regionid];
        ^result.merge(result2, { | e1, e2 | e2; }).merge(result3, { | e1, e2 | e2; });
    }

    // private methods
    pr_eqPower {
        | x, l, h |
        ^sqrt(((x-l)/(h-l)).clip(0,1));
    }

    pr_calcXf {
        | masterid, groupid, regionid, freq, amp |
        var vel = amp*127;
        var xfv = 1;
        var inlovel = this.getProperty(masterid, groupid, regionid, \xfin_lovel, nil);
        var inhivel = this.getProperty(masterid, groupid, regionid, \xfin_hivel, nil);
        var outlovel = this.getProperty(masterid, groupid, regionid, \xfout_lovel, nil);
        var outhivel = this.getProperty(masterid, groupid, regionid, \xfout_hivel, nil);
        var xfn = 1;
        var inlokey = this.getProperty(masterid, groupid, regionid, \xfin_lokey, nil, true);
        var inhikey = this.getProperty(masterid, groupid, regionid, \xfin_hikey, nil, true);
        var outlokey = this.getProperty(masterid, groupid, regionid, \xfout_lokey, nil, true);
        var outhikey = this.getProperty(masterid, groupid, regionid, \xfout_hikey, nil, true);
        if (inlovel.notNil && inhivel.notNil) {
            xfv = xfv*this.pr_eqPower(vel, inlovel, inhivel);
        };
        if (outlovel.notNil && outhivel.notNil) {
            xfv = xfv*this.pr_eqPower(vel, outlovel, outhivel);
        };
        if (inlokey.notNil && inhikey.notNil) {
            xfn = xfn*this.pr_eqPower(vel, inlokey, inhikey);
        };
        if (outlokey.notNil && outhikey.notNil) {
            xfn = xfn*this.pr_eqPower(vel, outlokey, outhikey);
        };
        ^(xfv*xfn);
    }

    pr_getActiveRegions {
        | freq, amp |
        // todo extend with more parameters
        var velocity = (amp*127).clip(0,127);
        var midinote = freq.cpsmidi.debug("requested midi note");
        var active_keys = [];
        this.sfzdata.keys.do({
            | globalid |
            this.sfzdata[globalid].keys.do({
                | masterid |
                this.sfzdata[globalid][masterid].keys.do({
                    | groupid |
                    this.sfzdata[globalid][masterid][groupid].keys.do({
                        | regionid |
                        var vel = this.getProperty(masterid, groupid, regionid, \vel, nil);
                        var lovel = this.getProperty(masterid, groupid, regionid, \lovel, 0);
                        var hivel = this.getProperty(masterid, groupid, regionid, \hivel, 127);
                        var key = this.getProperty(masterid, groupid, regionid, \key, nil, true);
                        var lokey = this.getProperty(masterid, groupid, regionid, \lokey, nil, true);
                        var hikey = this.getProperty(masterid, groupid, regionid, \hikey, nil, true);
                        var pitch_keycenter = this.getProperty(masterid, groupid, regionid, \pitch_keycenter, nil, true);
                        var velOk = false;
                        var noteOk = false;
                        var sampleOk = false;
                        var buffnote = nil;
                        if (vel.notNil) {
                            if (velocity == vel.asFloat) {
                                velOk = true;
                            };
                        } {
                            if (vel.isNil && hivel.notNil && lovel.notNil) {
                                if ((velocity <= hivel.asInteger) && (velocity >= lovel.asInteger)) {
                                    velOk = true;
                                };
                            };
                        };
                        if (key.notNil) {
                            if ((midinote.round(1).asInteger == key.asInteger)) {
                                noteOk = true;
                                buffnote = key;
                            };
                        } {
                            if (hikey.notNil && lokey.isNil) {
                                lokey = 0;
                            };
                            if (lokey.notNil && hikey.isNil) {
                                hikey = 127;
                            };
                            if (key.isNil && hikey.notNil && lokey.notNil) {
                                if ((midinote.floor.asInteger <= hikey.asInteger) && (midinote.floor.asInteger >= lokey.asInteger)) {
                                    noteOk = true;
                                    buffnote = pitch_keycenter ? 440.cpsmidi;
                                };
                            };
                        };


                        hikey.postln("hikey");
                        lokey.postln("lokey");
                        pitch_keycenter.postln("pitch_keycenter");
                        midinote.postln("midinote");
                        velOk.debug("velOk");
                        noteOk.debug("noteOk");

                        if (noteOk.and(velOk)) {
                            var lookupkey = (masterid ++ "_" ++ groupid ++ "_" ++ regionid).asSymbol;
                            if (this.getProperty(masterid, groupid, regionid, \sample, nil).notNil) {
                                lookupkey.debug("active region");
                                this.getProperty(masterid, groupid, regionid, \sample, nil).debug("sample");
                                active_keys = active_keys.add([buffnote, lookupkey, [masterid, groupid, regionid]]);
                            };
                        };
                    });
                });
            });
        });
        ^active_keys;
    }

    pr_loadFile {
        | path |
        this.path = path;
        this.filecontents = File.readAllString(path);
        this.filecontents = this.pr_resolveIncludes(path, this.filecontents);
        ^this.filecontents;
    }

    pr_resolveIncludes {
        | path, contents |
        var localcontents = contents.copy();
        var findincluderegex = "#include \"([a-zA-Z0-9.]+)\"[ \t]*[\r\n]*"; // with capture group
        var replaceincluderegex = "#include \"[a-zA-Z0-9.]+\"[ \t]*[\r\n]*"; // no capture groups
        while ({localcontents.findRegexp(findincluderegex) != [ ]}) {
            var includefilename = localcontents.findRegexp(findincluderegex)[1][1];
            var includefilecontents = File.readAllString((PathName(path).pathOnly +/+ includefilename).debug("include"));
            var extrapath = "";
            var controlregex = includefilecontents.findRegexp("<control>[^<]*");
            if (controlregex != [ ]) {
                var controlsection = controlregex[0][1];
                var defaultpathregex = "default_path=([^ \t\r\n]+)";
                var defaultpathproperty;
                controlsection = controlsection.replace("<control>", "");
                defaultpathproperty = controlsection.findRegexp(defaultpathregex);
                if (defaultpathproperty != [ ]) {
                    extrapath = defaultpathproperty[1][1].replace("\\","/");
                };
                includefilecontents = includefilecontents.replaceRegex("<control>[^<]*", ""); // and remove rest of control section (not supported)
                includefilecontents = includefilecontents.replaceRegex("sample=","sample="++extrapath); // only take into account default_path
            };
            localcontents = localcontents.replaceRegex(replaceincluderegex, includefilecontents, 0);
        };
        ^localcontents;
    }

    pr_removeComments {
        | string |
        var result;
        result = string.replaceRegex(SfzReader.regex_comment, "");
        ^result;
    }

    pr_compressMultiWhitespace {
        | string |
        ^string.replaceRegex(SfzReader.regex_ws, " ");
    }

    pr_removeMultiWhitespace {
        | string |
        ^string.replaceRegex(SfzReader.regex_ws, "");
    }

    pr_removeEmpties {
        | list |
        ^list.removeAllSuchThat({ |el| this.pr_removeMultiWhitespace(el).compare("") != 0; });
    }

    pr_parseContents {
        var groupid = 0;
        var regionid = 0;
        var globals,globals_clean;
        var control_properties;
        var control_section;
        var filecontents = this.filecontents;
        // cleanup file contents: remove comments and replace many whitespace with single space
        filecontents = this.pr_compressMultiWhitespace(this.pr_removeComments(filecontents));
        // split off <control> from the rest
        this.control = ();
        control_section = filecontents.betweenRegex("<control>", "<", 0, 1);
        if (control_section.notNil) {
            var cutpos;
            control_properties = this.pr_removeEmpties(control_section.split($ ));
            control_properties.do({
                | cprop_contents, cpropid |
                var keyval = cprop_contents.split($=);
                if (keyval[1].notNil) {
                    if (keyval[1].stripWhiteSpace.compare("") != 0) {
                        this.control[keyval[0].asSymbol] = keyval[1].stripWhiteSpace;
                    }
                };
            });
            cutpos = filecontents.betweenRegexPos("<control>", "<", 0, 1);
            filecontents = filecontents.copyRange(cutpos[1], filecontents.size-1);
        };

        // cleanup rests of previous parsing
        this.sfzdata = ();
        // split on <global>
        globals = filecontents.splitRegex("<global>");
        globals_clean = this.pr_removeEmpties(globals);
        globals_clean.do({
            | filecontents, globalid |
            var masters, masters_clean;
            this.sfzdata[globalid.asSymbol] = ();
            // split on <master>
            masters = filecontents.splitRegex("<master>");
            masters_clean = this.pr_removeEmpties(masters);
            masters_clean.do({
                | filecontents, masterid |
                var groups, groups_clean;
                this.sfzdata[globalid.asSymbol][masterid.asSymbol] = ();
                // split on <group>
                groups = filecontents.splitRegex("<group>");
                groups_clean = this.pr_removeEmpties(groups);
                groups_clean.do({
                    | contents, groupid |
                    var regions, regions_clean;
                    this.sfzdata[globalid.asSymbol][masterid.asSymbol][groupid.asSymbol] = ();
                    // split on <region>
                    regions = contents.splitRegex("<region>");
                    regions_clean = this.pr_removeEmpties(regions);
                    regions_clean.do({
                        | regioncontents, regionid |
                        var properties;
                        this.sfzdata[globalid.asSymbol][masterid.asSymbol][groupid.asSymbol][regionid.asSymbol] = ();
                        // split on whitespace
                        properties = this.pr_removeEmpties(regioncontents.split($ ));
                        properties.do({
                            | propertycontents, propertyid |
                            var keyval = propertycontents.split($=);
                            if (keyval[1].notNil) {
                                if (keyval[1].stripWhiteSpace.compare("") != 0) {
                                    this.sfzdata[globalid.asSymbol][masterid.asSymbol][groupid.asSymbol][regionid.asSymbol][keyval[0].asSymbol] = keyval[1].stripWhiteSpace;
                                };
                            } {
                                keyval.debug("not sure what to do with this weird keyval");
                            };
                        });
                    });
                });
            });
        });

        ^this.sfzdata;
    }

    pr_createSynthDefs {
        SynthDef((this.id++"playbuf1").asSymbol, {
            | out=0, bufnum=(1.neg), gate=0, rate=1, offset=0, stloop=0, endloop=0, amp=0.5, volume=0, pan=0, xf=1,
            ampeg_start=0, ampeg_delay=0, ampeg_attack=0.01, ampeg_sustain=1, ampeg_decay=0.01, ampeg_release=1,
            ampeg_vel2delay=0, ampeg_vel2attack=0, ampeg_vel2sustain=0, ampeg_vel2decay=0, ampeg_vel2release=0,
            ampeg_hold=0, ampeg_vel2hold=0 |
            var envspec = Env.dadsr(delayTime:(ampeg_delay + (ampeg_vel2delay*amp)),
                attackTime:(ampeg_attack + (ampeg_vel2attack*amp)),
                decayTime:(ampeg_decay + (ampeg_vel2decay*amp)),
                sustainLevel:(ampeg_sustain + (ampeg_vel2sustain*amp)),
                releaseTime:(ampeg_release + (ampeg_vel2release*amp))
            );
            var env = EnvGen.ar(envspec, gate:gate, doneAction:Done.freeSelf);
            var sig = Pan2.ar(PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum)*rate, 1, offset, 0, 0)*env*amp*volume.dbamp*xf, pan);
            Out.ar(out, sig);
        }).add;
        this.synths = this.synths.add((this.id++"playbuf1").asSymbol);

        SynthDef((this.id++"loopbuf1").asSymbol, {
            | out=0, bufnum=(1.neg), gate=0, rate=1, offset=0, stloop=0, endloop=0, amp=0.5, volume=0, pan=0, xf=1,
            ampeg_start=0, ampeg_delay=0, ampeg_attack=0.01, ampeg_sustain=1, ampeg_decay=0.01, ampeg_release=1,
            ampeg_vel2delay=0, ampeg_vel2attack=0, ampeg_vel2sustain=0, ampeg_vel2decay=0, ampeg_vel2release=0,
            ampeg_hold=0, ampeg_vel2hold=0 |
            var envspec = Env.dadsr(delayTime:(ampeg_delay + (ampeg_vel2delay*amp)),
                attackTime:(ampeg_attack + (ampeg_vel2attack*amp)),
                decayTime:(ampeg_decay + (ampeg_vel2decay*amp)),
                sustainLevel:(ampeg_sustain + (ampeg_vel2sustain*amp)),
                releaseTime:(ampeg_release + (ampeg_vel2release*amp))
            );
            var env = EnvGen.ar(envspec, gate:gate, doneAction:Done.freeSelf);
            var sig = Pan2.ar(LoopBuf.ar(1,bufnum,BufRateScale.kr(bufnum)*rate,1,offset,stloop,endloop)*env*amp*volume.dbamp*xf, pan);
            Out.ar(out, sig);
        }).add;
        this.synths = this.synths.add((this.id++"loopbuf1").asSymbol);

        SynthDef((this.id++"playbuf2").asSymbol, {
            | out=0, bufnum=(1.neg), gate=0, rate=1, offset=0, stloop=0, endloop=0, amp=0.5, volume=0, pan=0, xf=1,
            ampeg_start=0, ampeg_delay=0, ampeg_attack=0.01, ampeg_sustain=1, ampeg_decay=0.01, ampeg_release=1,
            ampeg_vel2delay=0, ampeg_vel2attack=0, ampeg_vel2sustain=0, ampeg_vel2decay=0, ampeg_vel2release=0,
            ampeg_hold=0, ampeg_vel2hold=0 |
            var envspec = Env.dadsr(delayTime:(ampeg_delay + (ampeg_vel2delay*amp)),
                attackTime:(ampeg_attack + (ampeg_vel2attack*amp)),
                decayTime:(ampeg_decay + (ampeg_vel2decay*amp)),
                sustainLevel:(ampeg_sustain + (ampeg_vel2sustain*amp)),
                releaseTime:(ampeg_release + (ampeg_vel2release*amp))
            );
            var env = EnvGen.ar(envspec, gate:gate, doneAction:Done.freeSelf);
            var bufplay = PlayBuf.ar(2,bufnum,BufRateScale.kr(bufnum)*rate,1,offset,0, 0)*env*amp*volume.dbamp*xf;
            var sig = Balance2.ar(bufplay[0], bufplay[1], pan);
            Out.ar(out, sig);
        }).add;
        this.synths = this.synths.add((this.id++"playbuf2").asSymbol);

        SynthDef((this.id++"loopbuf2").asSymbol, {
            | out=0, bufnum=(1.neg), gate=0, rate=1, offset=0, stloop=0, endloop=0, amp=0.5, volume=0, pan=0, xf=1,
            ampeg_start=0, ampeg_delay=0, ampeg_attack=0.01, ampeg_sustain=1, ampeg_decay=0.01, ampeg_release=1,
            ampeg_vel2delay=0, ampeg_vel2attack=0, ampeg_vel2sustain=0, ampeg_vel2decay=0, ampeg_vel2release=0,
            ampeg_hold=0, ampeg_vel2hold=0 |
            var envspec = Env.dadsr(delayTime:(ampeg_delay + (ampeg_vel2delay*amp)),
                attackTime:(ampeg_attack + (ampeg_vel2attack*amp)),
                decayTime:(ampeg_decay + (ampeg_vel2decay*amp)),
                sustainLevel:(ampeg_sustain + (ampeg_vel2sustain*amp)),
                releaseTime:(ampeg_release + (ampeg_vel2release*amp))
            );
            var env = EnvGen.ar(envspec, gate:gate, doneAction:Done.freeSelf);
            var bufplay = LoopBuf.ar(2,bufnum,BufRateScale.kr(bufnum)*rate,1,offset,stloop,endloop)*env*amp*volume.dbamp*xf;
            var sig = Balance2.ar(bufplay[0], bufplay[1], pan);
            Out.ar(out, sig);
        }).add;
        this.synths = this.synths.add((this.id++"loopbuf2").asSymbol);
    }

    pr_loadBuffers {
        | server |
        "about to load".postln;
        this.buffers = ();
        this.sfzdata.keys.do({
            |globalid|
            this.sfzdata[globalid].keys.do({
                | masterid |
                this.sfzdata[globalid][masterid].keys.do({
                    | groupid|
                    this.sfzdata[globalid][masterid][groupid].keys.do({
                        |regionid|
                        var samplepath = this.getProperty(masterid, groupid, regionid, \sample);
                        if (samplepath.notNil) {
                            var buf;
                            var fullpath = (PathName(this.path).pathOnly +/+ samplepath);
                            var key = (masterid ++ "_" ++ groupid ++ "_" ++ regionid).asSymbol;
                            samplepath = samplepath.replace("\\", "/");
                            buf = Buffer.read(server, fullpath);
                            server.sync;
                            this.buffers[key] = buf;
                            ("Loading" + key ++ ": " ++ samplepath).postln;
                        };
                    });
                });
            });
        });
    }
}

