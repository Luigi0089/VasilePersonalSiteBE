package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.SkillDto;
import it.vasilepersonalsite.DTO.CategoryDto;
import it.vasilepersonalsite.DTO.KeywordDto;

import java.util.List;

public interface SkillService {

    // ===== SKILL =====
    List<SkillDto> findAllSkills();
    SkillDto findSkillById(Long id);
    SkillDto createSkill(SkillDto skillDto, String passsword);
    SkillDto updateSkill(Long id, SkillDto updates, String password);
    void deleteSkill(Long id, String password);

    // ===== CATEGORY =====
    List<CategoryDto> findAllCategories();
    CategoryDto findCategoryById(Long id);
    CategoryDto createCategory(CategoryDto dto, String password);
    CategoryDto updateCategory(Long id, CategoryDto dto, String password);
    void deleteCategory(Long id, String password);

    // ===== KEYWORD =====
    List<KeywordDto> findAllKeywords();
    KeywordDto findKeywordById(Long id);
    KeywordDto createKeyword(KeywordDto dto, String password);
    KeywordDto updateKeyword(Long id, KeywordDto dto, String password);
    void deleteKeyword(Long id, String password);
}
