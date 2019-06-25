(
s.waitForBoot({
    ~test = SfzReader();
    ~test.load(s, "/home/shimpe/documents/music/soundfonts/vpo/Virtual-Playing-Orchestra3/Woodwinds/flute-SOLO-sustain.sfz");
    fork {
        // play some normal notes
        Panola("a4_8 b4 g4 c5 f4 g4 a4_4").midinotePattern.asStream.all.do({
            |note|
            ~test.play(0, note.midicps, 0.3, 1.0, 0.8);
            0.5.wait;
        });

        // some microtones
        [440,550,660,770,880,990].mirror2.mirror2.do({
            |note|
            ~test.play(0, note, 0.1, 0.25, 0.8);
            0.25.wait;
        });

        5.wait;

        ~test.freeMemory;
    }

});
)
