package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.DTO.SkillDto;
import it.vasilepersonalsite.DTO.CategoryDto;
import it.vasilepersonalsite.DTO.KeywordDto;
import it.vasilepersonalsite.entity.Skill;
import it.vasilepersonalsite.entity.Category;
import it.vasilepersonalsite.entity.Keyword;
import it.vasilepersonalsite.DAO.SkillDao;
import it.vasilepersonalsite.DAO.CategoryDao;
import it.vasilepersonalsite.DAO.KeywordDao;
import it.vasilepersonalsite.exception.PasswordErrataException;
import it.vasilepersonalsite.service.SkillService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional
public class SkillServiceImpl implements SkillService {

    @Autowired
    private SkillDao skillDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private KeywordDao keywordDao;

    @Value("${universal.password}")
    private String universalPassword;

    // =======================
    //        SKILL
    // =======================

    @Override
    public List<SkillDto> findAllSkills() {
        return skillDao.findAllWithCategoriesAndKeywords()
                .stream()
                .map(SkillDto::new)
                .toList();
    }


    @Override
    public SkillDto findSkillById(Long id) {
        Skill skill = skillDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Skill non trovata con id " + id));
        return new SkillDto(skill);
    }

    @Override
    public SkillDto createSkill(SkillDto dto, String password) {
        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }
        // mappo il DTO in nuova entity skill
        Skill skill = mapSkillDtoToEntity(dto);
        // sicurezza: in create non voglio usare l'id del dto
        skill.setId(null);

        Skill saved = skillDao.save(skill);
        return new SkillDto(saved);
    }

    @Override
    public SkillDto updateSkill(Long id, SkillDto dto, String password) {
        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }
        Skill existing = skillDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Skill non trovata con id " + id));

        // campi semplici
        existing.setName(dto.getName());
        existing.setLevel(dto.getLevel());
        existing.setAcquired(dto.getAcquired());
        existing.setNotes(dto.getNotes());

        // categorie
        LinkedHashSet<Category> categories = new LinkedHashSet<>();
        if (dto.getCategories() != null) {
            for (String catName : dto.getCategories()) {
                if (catName == null || catName.isBlank()) continue;

                String normalized = catName.trim();
                Category category = categoryDao.findByNameIgnoreCase(normalized)
                        .orElseGet(() -> {
                            Category c = new Category();
                            c.setName(normalized);
                            return categoryDao.save(c);
                        });
                categories.add(category);
            }
        }
        existing.setCategories(categories);

        // keyword
        LinkedHashSet<Keyword> keywords = new LinkedHashSet<>();
        if (dto.getKeywords() != null) {
            for (String kwValue : dto.getKeywords()) {
                if (kwValue == null || kwValue.isBlank()) continue;

                String normalized = kwValue.trim();
                Keyword keyword = keywordDao.findByValueIgnoreCase(normalized)
                        .orElseGet(() -> {
                            Keyword k = new Keyword();
                            k.setValue(normalized);
                            return keywordDao.save(k);
                        });
                keywords.add(keyword);
            }
        }
        existing.setKeywords(keywords);

        Skill saved = skillDao.save(existing);
        return new SkillDto(saved);
    }

    @Override
    public void deleteSkill(Long id, String password) {
        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }
        if (!skillDao.existsById(id)) {
            throw new EntityNotFoundException("Skill non trovata con id " + id);
        }
        skillDao.deleteById(id);
    }

    /**
     * Mapping SkillDto -> Skill (nuova entity)
     * Usa il costruttore Skill(SkillDto) per i campi semplici
     * e risolve Category/Keyword tramite i DAO.
     */
    private Skill mapSkillDtoToEntity(SkillDto dto) {
        Skill skill = new Skill(dto);  // id/name/level/acquired/notes, set vuoti

        // categorie
        LinkedHashSet<Category> categories = new LinkedHashSet<>();
        if (dto.getCategories() != null) {
            for (String catName : dto.getCategories()) {
                if (catName == null || catName.isBlank()) continue;

                String normalized = catName.trim();
                Category category = categoryDao.findByNameIgnoreCase(normalized)
                        .orElseGet(() -> {
                            Category c = new Category();
                            c.setName(normalized);
                            return categoryDao.save(c);
                        });
                categories.add(category);
            }
        }
        skill.setCategories(categories);

        // keyword
        LinkedHashSet<Keyword> keywords = new LinkedHashSet<>();
        if (dto.getKeywords() != null) {
            for (String kwValue : dto.getKeywords()) {
                if (kwValue == null || kwValue.isBlank()) continue;

                String normalized = kwValue.trim();
                Keyword keyword = keywordDao.findByValueIgnoreCase(normalized)
                        .orElseGet(() -> {
                            Keyword k = new Keyword();
                            k.setValue(normalized);
                            return keywordDao.save(k);
                        });
                keywords.add(keyword);
            }
        }
        skill.setKeywords(keywords);

        return skill;
    }

    // =======================
    //       CATEGORY
    // =======================

    @Override
    public List<CategoryDto> findAllCategories() {
        return categoryDao.findAll(Sort.by("name"))
                .stream()
                .map(c -> new CategoryDto(c))
                .toList();
    }

    @Override
    public CategoryDto findCategoryById(Long id) {
        Category category = categoryDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria non trovata con id " + id));

        return new CategoryDto(category);
    }

    @Override
    public CategoryDto createCategory(CategoryDto dto, String password) {
        String name = dto.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome categoria obbligatorio");
        }
        String normalized = name.trim();

        Category category = categoryDao.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(normalized);
                    return categoryDao.save(c);
                });

        return new CategoryDto(category);
    }

    @Override
    public CategoryDto updateCategory(Long id, CategoryDto dto, String password) {
        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }
        Category category = categoryDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria non trovata con id " + id));

        String newName = dto.getName();
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Nome categoria obbligatorio");
        }
        String normalized = newName.trim();

        categoryDao.findByNameIgnoreCase(normalized)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Esiste già una categoria con nome: " + normalized);
                });

        category.setName(normalized);
        Category saved = categoryDao.save(category);

        return new CategoryDto(category);
    }

    @Override
    public void deleteCategory(Long id, String password) {

        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }

        Category category = categoryDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria non trovata con id " + id));

        if (!category.getSkills().isEmpty()) {
            throw new IllegalStateException("Impossibile eliminare la categoria: è ancora associata a delle skill");
        }

        categoryDao.delete(category);
    }

    // =======================
    //        KEYWORD
    // =======================

    @Override
    public List<KeywordDto> findAllKeywords() {
        return keywordDao.findAll(Sort.by("value"))
                .stream()
                .map(k -> KeywordDto.builder()
                        .id(k.getId())
                        .value(k.getValue())
                        .build()
                )
                .toList();
    }

    @Override
    public KeywordDto findKeywordById(Long id) {
        Keyword keyword = keywordDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Keyword non trovata con id " + id));

        return KeywordDto.builder()
                .id(keyword.getId())
                .value(keyword.getValue())
                .build();
    }

    @Override
    public KeywordDto createKeyword(KeywordDto dto, String password) {
        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }
        String value = dto.getValue();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Valore keyword obbligatorio");
        }
        String normalized = value.trim();

        Keyword keyword = keywordDao.findByValueIgnoreCase(normalized)
                .orElseGet(() -> {
                    Keyword k = new Keyword();
                    k.setValue(normalized);
                    return keywordDao.save(k);
                });

        return KeywordDto.builder()
                .id(keyword.getId())
                .value(keyword.getValue())
                .build();
    }

    @Override
    public KeywordDto updateKeyword(Long id, KeywordDto dto, String password) {
        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }
        Keyword keyword = keywordDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Keyword non trovata con id " + id));

        String newValue = dto.getValue();
        if (newValue == null || newValue.isBlank()) {
            throw new IllegalArgumentException("Valore keyword obbligatorio");
        }
        String normalized = newValue.trim();

        keywordDao.findByValueIgnoreCase(normalized)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Esiste già una keyword con valore: " + normalized);
                });

        keyword.setValue(normalized);
        Keyword saved = keywordDao.save(keyword);

        return KeywordDto.builder()
                .id(saved.getId())
                .value(saved.getValue())
                .build();
    }

    @Override
    public void deleteKeyword(Long id, String password) {
        if (!password.equals(universalPassword)) {
            throw new PasswordErrataException();
        }
        Keyword keyword = keywordDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Keyword non trovata con id " + id));

        if (!keyword.getSkills().isEmpty()) {
            throw new IllegalStateException("Impossibile eliminare la keyword: è ancora associata a delle skill");
        }

        keywordDao.delete(keyword);
    }
}
