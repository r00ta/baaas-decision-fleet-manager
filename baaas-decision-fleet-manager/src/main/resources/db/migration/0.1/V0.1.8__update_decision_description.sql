ALTER TABLE decision_version add description text;

UPDATE decision_version dv set description = (select description from decision d where d.id = dv.decision_id);

ALTER TABLE decision_version ALTER COLUMN description SET NOT NULL;
 
ALTER TABLE decision drop column description;
