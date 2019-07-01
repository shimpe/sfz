(
o = Server.local.options;
o.numBuffers_(16000);
o.memSize_(8192*100);
s.reboot;
s.waitForBoot({
    ~test = SfzReader();
    //~test.load(s, "/home/shimpe/documents/music/soundfonts/vpo/Virtual-Playing-Orchestra3/Woodwinds/flute-SOLO-sustain.sfz");
    //~test.load(s, "/home/shimpe/documents/music/soundfonts/salamanderpiano/SalamanderGrandPianoV3_44.1khz16bit/SalamanderGrandPianoV3.sfz");
    //~test.load(s, "/home/shimpe/documents/music/soundfonts/nobudget/sfz/melodica/Melodica/melodica.sfz");
    //~test.load(s, "/home/shimpe/documents/music/soundfonts/nobudget/sfz/60s_organ/Farfisa/60s_organ.sfz");
    //~test.load(s, "/home/shimpe/documents/music/soundfonts/nobudget/sfz/acousbass/AcousBass/acous_bass.sfz");
    ~test.load(s, "/home/shimpe/documents/music/soundfonts/nobudget/sfz/altosax/AltoSax/altosax.sfz");
    fork {
        // play some normal notes
        Panola("a1_8 b1 g1 c2 f1 g1 a1_4").midinotePattern.asStream.all.do({
            |note|
            ~test.play(out:0, freq:note.midicps, amp:0.4, dur:2.0, legato:1.0);
            "next".postln;
            2.0.wait;
        });

        // some microtones
        [440,550,660,770,880,990].mirror2.mirror2.do({
            |freq, idx|
            ~test.play(out:0, freq:freq, amp:(0.4 + ((0.6/25)*idx)).debug("amp"), dur:2.0, legato:1.0);
            2.0.wait;
        });

        5.wait;

        ~test.freeMemory;
    }

});
)
