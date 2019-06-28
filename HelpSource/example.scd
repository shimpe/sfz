(
o = Server.local.options;
o.numBuffers_(16000);
o.memSize_(8192*100);
s.reboot;
s.waitForBoot({
    ~test = SfzReader();
    //~test.load(s, "/home/shimpe/documents/music/soundfonts/vpo/Virtual-Playing-Orchestra3/Woodwinds/flute-SOLO-sustain.sfz");
    //~test.load(s, "/home/shimpe/documents/music/soundfonts/salamanderpiano/SalamanderGrandPianoV3_44.1khz16bit/SalamanderGrandPianoV3.sfz");
    ~test.load(s, "/home/shimpe/documents/music/soundfonts/nobudget/sfz/melodica/Melodica/melodica.sfz");
    fork {
        // play some normal notes
        Panola("a4_8 b4 g4 c5 f4 g4 a4_4").midinotePattern.asStream.all.do({
            |note|
            ~test.play(out:0, freq:note.midicps, amp:0.9, dur:0.5, legato:0.8);
            0.5.wait;
        });

        // some microtones
        [440,550,660,770,880,990].mirror2.mirror2.do({
            |freq, idx|
            ~test.play(out:0, freq:freq, amp:(0.4 + ((0.6/25)*idx)).debug("amp"), dur:0.25, legato:0.8);
            0.25.wait;
        });

        5.wait;

        ~test.freeMemory;
    }

});
)

// 67

// 0.9*127