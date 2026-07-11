/**
 * Exports every skill pack (and agent-written learning) from the skill_packs table as
 * individual markdown files, bundled into a single downloadable zip. Each file is
 * reconstructed with YAML frontmatter (name, description, kind, tags, confidence,
 * source, enabled) followed by the pack content - so a human tester can review the
 * current skill set outside the app.
 *
 * Run this from Servoy Developer (right-click the method -> Run) or wire it to a button.
 * In NG/Titanium client it downloads the zip in the browser; in smart client it prompts
 * for a save location.
 *
 * @param {String} [serverName] The Servoy server holding skill_packs. Defaults to 'ttyd'.
 * @return {Number} The number of skill files exported.
 *
 * @properties={typeid:24,uuid:"4AA66BE1-97DC-4E1B-8A4D-3E1DE4930E8D"}
 */
function exportSkillPacks(serverName) {

	serverName = serverName || 'ttyd';

	var sql = 'SELECT name, description, content, kind, tags, confidence, source, enabled FROM skill_packs ORDER BY kind, name';
	var ds;
	try {
		ds = databaseManager.getDataSetByQuery(serverName, sql, null, 2000);
	} catch (e) {
		plugins.dialogs.showErrorDialog('Export failed', 'Could not read skill_packs from server "' + serverName + '":\n' + e['message']);
		return 0;
	}

	if (ds.getMaxRowIndex() == 0) {
		plugins.dialogs.showInfoDialog('Export skill packs', 'No skill packs found on server "' + serverName + '".');
		return 0;
	}

	// build a zip in memory: one .md file per skill pack
	var baos = new Packages.java.io.ByteArrayOutputStream();
	var zos = new Packages.java.util.zip.ZipOutputStream(baos);
	var seen = {};

	for (var i = 1; i <= ds.getMaxRowIndex(); i++) {
		var row = ds.getRowAsArray(i); // [name, description, content, kind, tags, confidence, source, enabled]
		var md = buildSkillMarkdown(row);

		// filename: <kind>_<slug>.md, de-duplicated if needed
		var base = (row[3] || 'pack') + '_' + slug(row[0]);
		var fileName = base + '.md';
		var n = 1;
		while (seen[fileName]) {
			fileName = base + '_' + (++n) + '.md';
		}
		seen[fileName] = true;

		zos.putNextEntry(new Packages.java.util.zip.ZipEntry(fileName));
		var bytes = new Packages.java.lang.String(md).getBytes('UTF-8');
		zos.write(bytes, 0, bytes.length);
		zos.closeEntry();
	}
	zos.close();

	var count = ds.getMaxRowIndex();
	plugins.file.writeFile('skill_packs_export.zip', baos.toByteArray());
	application.output('Exported ' + count + ' skill pack(s) from "' + serverName + '" to skill_packs_export.zip', LOGGINGLEVEL.INFO);
	return count;
}

/**
 * Reconstructs one skill pack row as a markdown document with YAML frontmatter.
 * @private
 * @param {Array} row [name, description, content, kind, tags, confidence, source, enabled]
 * @return {String}
 * @properties={typeid:24,uuid:"65CDBBAE-9296-42B0-B96B-7331343E1930"}
 */
function buildSkillMarkdown(row) {
	var name = row[0];
	var description = row[1];
	var content = row[2] || '';
	var kind = row[3];
	var tags = row[4];
	var confidence = row[5];
	var source = row[6];
	var enabled = row[7];

	var fm = '---\n';
	fm += 'name: ' + name + '\n';
	fm += 'description: ' + (description || '') + '\n';
	fm += 'kind: ' + (kind || '') + '\n';
	if (tags) {
		fm += 'tags: ' + tags + '\n';
	}
	if (confidence !== null && confidence !== undefined) {
		fm += 'confidence: ' + confidence + '\n';
	}
	if (source) {
		fm += 'source: ' + source + '\n';
	}
	fm += 'enabled: ' + (enabled === false || enabled === 0 ? 'false' : 'true') + '\n';
	fm += '---\n\n';

	return fm + content + '\n';
}

/**
 * Turns a skill name into a filesystem-safe slug for the exported filename.
 * @private
 * @param {String} s
 * @return {String}
 * @properties={typeid:24,uuid:"DAD7DF8A-EA5C-41B0-B73F-113932B167B7"}
 */
function slug(s) {
	return ('' + s).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'skill';
}
