SfzReader {
    var <>path;
    var <>filecontents;
    var <>groups;

    *new {
        ^super.new.init();
    }

    init {
        this.path = "";
        this.filecontents = "";
        this.groups = ();
    }

    load {
        | path |
        this.pr_loadfile(path);
        this.pr_parsecontents;
        ^this.filecontents;
    }

    get_property {
        | groupid, regionid, property |
        groupid = groupid.asSymbol;
        regionid= regionid.asSymbol;
        if (this.groups[groupid].isNil) {
            ^nil;
        };
        if (this.groups[groupid][regionid].isNil) {
            regionid = \0;
        };
        if (this.groups[groupid][regionid].isNil) {
            ^nil;
        };
        if (this.groups[groupid][regionid][property].isNil) {
            if (this.groups[groupid][\0][property].isNil) {
                ^nil;
            } {
                ^this.groups[groupid][\0][property];
            };
        } {
            ^this.groups[groupid][regionid][property];
        };
    }

    list_properties {
        | groupid, regionid |
        var result, result2;
        groupid = groupid.asSymbol;
        regionid = regionid.asSymbol;
        result = this.groups[groupid][\0];
        result2 = this.groups[groupid][regionid];
        ^result.merge(result2, { | e1, e2 | e2; });
    }

    // private methods

    pr_loadfile {
        | path |
        this.path = path;
        this.filecontents = File.readAllString(path);
        ^this.filecontents;
    }

    pr_removeComments {
        | string |
        var result;
        result = string.replaceRegex("//[^\n]*\n", "");
        ^result;
    }

    pr_removeMultiWhitespace {
        | string |
        ^string.replaceRegex("[ \t\n]+", " ");
    }

    pr_removeEmpties {
        | list |
        ^list.removeAllSuchThat({ |el| el.compare("") != 0 });
    }

    pr_parsecontents {
        var groupid = 0;
        var regionid = 0;
        var groups;
        var groups_clean;
        // cleanup rests of previous parsing
        this.groups = ();

        // cleanup file contents: remove comments and replace many whitespace with single space
        this.filecontents = this.pr_removeMultiWhitespace(this.pr_removeComments(this.filecontents));

        // split on <group>
        groups = this.filecontents.splitRegex("<group>");
        groups_clean = this.pr_removeEmpties(groups);
        //groups_clean.debug("groups clean");
        groups_clean.do({
            | contents, groupid |
            var regions, regions_clean;
            this.groups[groupid.asSymbol] = ();
            //contents.debug("contents");
            regions = contents.splitRegex("<region>");
            regions_clean = this.pr_removeEmpties(regions);
            //regions_clean.debug("regions clean");
            regions_clean.do({
                | regioncontents, regionid |
                var properties;
                this.groups[groupid.asSymbol][regionid.asSymbol] = ();
                properties = this.pr_removeEmpties(regioncontents.split($ ));
                //properties.debug("properties for group"+groupid+"and region"+regionid);//properties.debug("properties for group"+groupid+"and region"+regionid);
                properties.do({
                    | propertycontents, propertyid |
                    var keyval = propertycontents.split($=);
                    this.groups[groupid.asSymbol][regionid.asSymbol][keyval[0].asSymbol] = keyval[1];
                });
            });
        });

        ^this.groups;
    }
}

