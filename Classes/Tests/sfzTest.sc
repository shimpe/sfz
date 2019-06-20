SfzReaderTest : UnitTest {
	test_check_classname {
		var reader = SfzReader();
		this.assert(reader.class == SfzReader);
	}

    test_loadfile {
        var reader = SfzReader();
        var expectedcontents = "<group>\n"
        "ampeg_attack=0.04 ampeg_release=0.45\n"
        "\n"
        "<region> sample=trumpet_pp_c4.wav key=c4\n"
        "<region> sample=trumpet_pp_c#4.wav key=c#4\n"
        "<region> sample=trumpet_pp_d4.wav key=d4\n"
        "<region> sample=trumpet_pp_d#4.wav key=d#4\n"
        "\n"
        "<group>\n"
        "lovel=60\n"
        "<region> sample=trumpet_pp_e4.wav key=e4 // previous group parameters reset\n"
        "\n"
        "//Comments\n"
        "//Comment lines can be inserted anywhere inside the file. A comment line starts with the slash character ('/'), and it extends till the end of the line.\n"
        "\n"
        "<region> \n"
        "sample=trumpet_pp_c4.wav\n"
        "// middle C in the keyboard\n"
        "lokey=60\n"
        "// pianissimo layer\n"
        "lovel=0 hivel=20 // another comment\n";
        var readcontents = reader.pr_loadfile((PathName(SfzReaderTest.filenameSymbol.asString).pathOnly +/+ "testfiles/1.txt").debug("path"));
        this.assert(readcontents.compare(expectedcontents) == 0);
    }

    test_parse1 {
        var reader = SfzReader();
        var contents = reader.pr_loadfile((PathName(SfzReaderTest.filenameSymbol.asString).pathOnly +/+ "testfiles/1.txt").debug("path"));
        var groups = reader.pr_parsecontents;
        this.assert(groups[\0][\0][\ampeg_attack] == "0.04");
        this.assert(groups[\0][\0][\ampeg_release] == "0.45");
        this.assert(groups[\0][\1][\sample] == "trumpet_pp_c4.wav");
        this.assert(groups[\0][\1][\key] == "c4");
        this.assert(groups[\0][\2][\sample] == "trumpet_pp_c#4.wav");
        this.assert(groups[\0][\2][\key] == "c#4");
        this.assert(groups[\0][\3][\sample] == "trumpet_pp_d4.wav");
        this.assert(groups[\0][\3][\key] == "d4");
        this.assert(groups[\0][\4][\sample] == "trumpet_pp_d#4.wav");
        this.assert(groups[\0][\4][\key] == "d#4");
        this.assert(groups[\1][\0][\lovel] == "60");
        this.assert(groups[\1][\1][\sample] == "trumpet_pp_e4.wav");
        this.assert(groups[\1][\1][\key] == "e4");
        this.assert(groups[\1][\2][\sample] == "trumpet_pp_c4.wav");
        this.assert(groups[\1][\2][\lokey] == "60");
        this.assert(groups[\1][\2][\lovel] == "0");
        this.assert(groups[\1][\2][\hivel] == "20");
    }

    test_get_property {
        var reader = SfzReader();
        var contents = reader.pr_loadfile((PathName(SfzReaderTest.filenameSymbol.asString).pathOnly +/+ "testfiles/1.txt").debug("path"));
        reader.pr_parsecontents;
        this.assert(reader.get_property(0, 0, \ampeg_attack) == "0.04");
        this.assert(reader.get_property(0, 1, \ampeg_attack) == "0.04"); // if no region property found, use group property instead
        this.assert(reader.get_property(0, 0, \what) == nil); // unknown property => return nil
        this.assert(reader.get_property(1, 1, \sample) == "trumpet_pp_e4.wav");
        this.assert(reader.get_property(1, 1, \ampeg_attack) == nil); // no group properties defined in 2nd group
        this.assert(reader.get_property(1, 2, \lovel) == "0"); // region property gets priority over group property
    }

    test_list_properties {
        var reader = SfzReader();
        reader.load((PathName(SfzReaderTest.filenameSymbol.asString).pathOnly +/+ "testfiles/1.txt"));
        this.assert(reader.list_properties(\0, \2).size == 4)
    }
}


SfzTester {
	*new {
		^super.new.init();
	}

	init {
		SfzReaderTest.run;
	}
}
