package jobfinder.tailor;

import java.util.List;

import jobfinder.profile.Profile;
import jobfinder.source.Job;

/**
 * CV content for one job. Nothing here is new text: skills, bullets and highlights are copied from the profile,
 * only selected and ordered by relevance to the job. Grounding.verify enforces this.
 */
public record TailoredCv(Job job, String headline, String summary, List<String> skills,
                         List<Profile.ProofPoint> highlights, List<Profile.Experience> experience) {}
