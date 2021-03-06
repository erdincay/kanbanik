package com.googlecode.kanbanik.client.components.task;

import java.util.Date;
import java.util.List;

import com.allen_sauer.gwt.dnd.client.DragController;
import com.allen_sauer.gwt.dnd.client.util.DragClientBundle;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.googlecode.kanbanik.client.KanbanikResources;
import com.googlecode.kanbanik.client.Modules;
import com.googlecode.kanbanik.client.api.DtoFactory;
import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.components.common.DataCollector;
import com.googlecode.kanbanik.client.components.filter.BoardsFilter;
import com.googlecode.kanbanik.client.components.task.tag.TagResizingPictureLoadHandler;
import com.googlecode.kanbanik.client.managers.ClassOfServicesManager;
import com.googlecode.kanbanik.client.managers.UsersManager;
import com.googlecode.kanbanik.client.messaging.Message;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.MessageListener;
import com.googlecode.kanbanik.client.messaging.messages.board.GetAllBoardsResponseMessage;
import com.googlecode.kanbanik.client.messaging.messages.board.GetBoardsRequestMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.*;
import com.googlecode.kanbanik.client.messaging.messages.task.ChangeTaskSelectionMessage.ChangeTaskSelectionParams;
import com.googlecode.kanbanik.client.modules.lifecyclelisteners.ModulesLifecycleListener;
import com.googlecode.kanbanik.client.modules.lifecyclelisteners.ModulesLyfecycleListenerHandler;
import static com.googlecode.kanbanik.client.api.Dtos.TaskDto;

import static com.googlecode.kanbanik.client.components.task.tag.TagConstants.*;

public class TaskGui extends Composite implements MessageListener<TaskDto>, ModulesLifecycleListener, ClickHandler {

    @UiField
	FocusPanel header;

    @UiField
    FocusPanel namePanel;

	@UiField
	Label ticketIdLabel;
	
	@UiField
	HTML dueDateLabel;
	
	@UiField
	Label nameLabel;

    @UiField
    TextArea nameLabelTextArea;

	@UiField
	PushButton editButton;
	
	@UiField
	PushButton deleteButton;
	
	@UiField
	FocusPanel assigneePicturePlace;
	
	@UiField
	FocusPanel wholePanel;

	@UiField
	FlowPanel mainPanel;


    @UiField
    HTMLPanel contentContainer;

	@UiField
	FlowPanel tagsPanel;

	HandlerRegistration imageHandle;
	
	private TaskDto taskDto;

    private BoardsFilter filter;

    private DragController dragController;

	private DataCollector<Dtos.BoardDto> boardsCollector = new DataCollector<>();

	@UiField
	Style style;
	
	private static final TaskGuiTemplates TEMPLATE = GWT.create(TaskGuiTemplates.class);

    public static final String SELECTED_STYLE = DragClientBundle.INSTANCE.css().selected();

    @Override
    public void onClick(ClickEvent event) {
        event.stopPropagation();
        event.preventDefault();
    }

    public interface TaskGuiTemplates extends SafeHtmlTemplates {
	     @Template("<div class=\"{0}\">{1}</div>")
	     SafeHtml messageWithLink(String style, String msg);
	   }

	
	private TaskSelectionChangeListener taskSelectionChangeListener = new TaskSelectionChangeListener();

    private TaskFilterChangeListener taskFilterChangeListener = new TaskFilterChangeListener();
	
	public interface Style extends CssResource {
		
		String selected();
		
		String unselected();
		
		String missedStyle();

		String tagStyle();

		String tagLabelStyle();

		String tagImageStyle();

		String clickableTag();
	}
	
	interface MyUiBinder extends UiBinder<Widget, TaskGui> {}
	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);


    private Dtos.BoardDto boardDto;

	private TaskEditingComponent taskEditingComponent;

	public TaskGui(TaskDto taskDto, Dtos.BoardDto boardDto, DragController dragController) {
        this.dragController = dragController;

		nameLabelTextArea = new TextArea();
        this.boardDto = boardDto;

		initWidget(uiBinder.createAndBindUi(this));
		
		editButton.getUpFace().setImage(new Image(KanbanikResources.INSTANCE.editButtonImage()));
		deleteButton.getUpFace().setImage(new Image(KanbanikResources.INSTANCE.deleteButtonImage()));
		
		this.taskDto = taskDto;
		MessageBus.registerListener(TaskEditedMessage.class, this);
        MessageBus.registerListener(FilterChangedMessage.class, taskFilterChangeListener);
		MessageBus.registerListener(TaskChangedMessage.class, this);
		MessageBus.registerListener(TaskDeletedMessage.class, this);
		MessageBus.registerListener(ChangeTaskSelectionMessage.class, taskSelectionChangeListener);
		MessageBus.registerListener(GetSelectedTasksRequestMessage.class, this);
        MessageBus.registerListener(GetTasksByPredicateRequestMessage.class, this);

		new ModulesLyfecycleListenerHandler(Modules.BOARDS, this);

		taskEditingComponent = new TaskEditingComponent(this, editButton, boardDto);
		new TaskDeletingComponent(this, deleteButton);

        wholePanel.addClickHandler(this);

		setupAccordingDto(taskDto);
	}
	
	public void setupAccordingDto(TaskDto taskDto) {
		header.setStyleName("task-class-of-service");
		header.getElement().getStyle().setBackgroundColor(getColorOf(taskDto));
        contentContainer.getElement().getStyle().setBackgroundColor("#ffffff");
        mainPanel.getElement().getStyle().setBackgroundColor("#ffffff");
		ticketIdLabel.setText(taskDto.getTicketId());
		nameLabel.setText(taskDto.getName());
		nameLabel.setTitle(taskDto.getName());
        nameLabelTextArea.setText(taskDto.getName());
        nameLabelTextArea.setTitle(taskDto.getName());

        if (boardDto.isFixedSizeShortDescription()) {
            nameLabel.getElement().getStyle().setDisplay(Display.NONE);
            nameLabelTextArea.getElement().getStyle().setDisplay(Display.BLOCK);
        } else {
			nameLabel.getElement().getStyle().setDisplay(Display.TABLE_CELL);
			nameLabelTextArea.getElement().getStyle().setDisplay(Display.NONE);
		}

		boolean showingPictureEnabled = boardDto.isShowUserPictureEnabled();
		boolean hasAssignee = taskDto.getAssignee() != null;
		boolean assigneeHasPictue = hasAssignee && taskDto.getAssignee().getPictureUrl() != null && !"".equals(taskDto.getAssignee().getPictureUrl());

		if (hasAssignee && assigneeHasPictue && showingPictureEnabled) {
			if (imageHandle != null) {
				imageHandle.removeHandler();
			}

            Dtos.UserDto newUser = DtoFactory.userDto();
            newUser.setPictureUrl(taskDto.getAssignee().getPictureUrl());

			Image picture = UsersManager.getInstance().getPictureFor(newUser);

			assigneePicturePlace.clear();
			assigneePicturePlace.add(picture);
			assigneePicturePlace.setTitle(taskDto.getAssignee().getRealName());
			assigneePicturePlace.getElement().getStyle().setDisplay(Display.BLOCK);
            nameLabel.setWidth("110px");
            nameLabelTextArea.setWidth("130px");
            picture.addClickHandler(this);
		} else {
			assigneePicturePlace.getElement().getStyle().setDisplay(Display.NONE);
            nameLabel.setWidth("190px");
            nameLabelTextArea.setWidth("180px");
		}

		setupDueDate(taskDto.getDueDate());

		setupTags(taskDto.getTaskTags());
	}

	private void setupTags(List<Dtos.TaskTag> tags) {
		tagsPanel.clear();

		if (tags == null) {
			return;
		}

		for (Dtos.TaskTag tag : tags) {
			tagsPanel.add(renderTag(tag));
		}
	}

	private Widget renderTag(final Dtos.TaskTag tag) {
		Widget res;
		String pictureUrl = tag.getPictureUrl();

		if (pictureUrl == null || "".equals(pictureUrl)) {
			FlowPanel tagPanel = new FlowPanel();
			tagPanel.addStyleName(style.tagStyle());
                        Label tagLabel = new Label(tag.getName());
                        tagLabel.addStyleName(style.tagLabelStyle());
			tagPanel.add(tagLabel);
			res = tagPanel;
		} else {
			final Image tagImage = new Image();
			tagImage.setVisible(false);
                        tagImage.addStyleName(style.tagImageStyle());
			tagImage.addLoadHandler(new TagResizingPictureLoadHandler(tagImage));

			tagImage.setUrl(pictureUrl);
			tagImage.setAltText(tagImage.getTitle());
			res = tagImage;
		}

		String color = tag.getColour();

		if (!(color == null || "".equals(color))) {
			int colorIndex = predefinedColors.indexOf(color);
			if (colorIndex == -1) {
				 color = "#" + color;
			}

			if (colorIndex != TRANSPARENT_INDEX) {
				res.getElement().getStyle().setBackgroundColor(color);
			}

		}

        String description = tag.getDescription();
        res.setTitle(description != null ? description : "No description provided");

		Dtos.TagClickTarget target = Dtos.TagClickTarget.from(tag.getOnClickTarget());
		boolean onClickDefined = tag.getOnClickUrl() != null && !"".equals(tag.getOnClickUrl());
		boolean targetDefined = target != Dtos.TagClickTarget.NONE;

		if (onClickDefined && targetDefined) {
			res.addStyleName(style.clickableTag());
			res.addDomHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Dtos.TagClickTarget target = Dtos.TagClickTarget.from(tag.getOnClickTarget());
					if (target == Dtos.TagClickTarget.NEW_WINDOW) {
						Window.open(tag.getOnClickUrl(), "_blank", "");
					}
				}
			}, ClickEvent.getType());
		}

		return res;
	}

	private void setupDueDate(String dueDate) {
		if (dueDate == null || "".equals(dueDate)) {
			dueDateLabel.setVisible(false);
			return;
		}
		
		Date date;
		try {
			date = DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).parse(dueDate);
		} catch(IllegalArgumentException e) {
			dueDateLabel.setVisible(false);
			return;
		}

		dueDateLabel.setVisible(true);
		setupHumanReadableDueDateText(dueDate, date);
	}

	@SuppressWarnings("deprecation")
	private void setupHumanReadableDueDateText(String dueDateText, Date dueDate) {
		Date nowDate = new Date();
		// comparing days only - not care about the time
		nowDate.setMinutes(0);
		nowDate.setHours(0);
		nowDate.setSeconds(0);
		
		final long DAY_MILLIS = 1000 * 60 * 60 * 24;
		
	    long day1 = dueDate.getTime() / DAY_MILLIS;
	    long day2 = nowDate.getTime() / DAY_MILLIS;
	    long diff = day1 - day2;
	    
	    if (diff < 0) {
	    	dueDateLabel.setTitle("Due date deadline (" + dueDateText + ") is already missed!");
	    	dueDateLabel.setHTML(TEMPLATE.messageWithLink(style.missedStyle(), "missed!"));
	    	return;
	    }
	    
	    if (diff == 0) {
	    	dueDateLabel.setTitle("Due date deadline ("+dueDateText+") is today!");
	    	dueDateLabel.setText("today");
	    	return;
	    }
	    
	    dueDateLabel.setTitle("Due date deadline ("+dueDateText+") is in " + diff + " days.");
	    if (diff > 365) {
	    	dueDateLabel.setText(" > year");
	    	return;
	    }
	    
	    if (diff > 31) {
	    	dueDateLabel.setText(" > month");
	    	return;
	    }
	    
	    if (diff > 7) {
	    	dueDateLabel.setText(" > week");
	    	return;
	    }
	    
	    if (diff == 1) {
	    	dueDateLabel.setText("tomorrow");
	    	return;
	    }
	    
    	dueDateLabel.setText(diff + " days");
	}

	private String getColorOf(TaskDto taskDto) {
		if (taskDto.getClassOfService() == null) {
			return "#" + ClassOfServicesManager.getInstance().getDefaultClassOfService().getColour();
		}
		
		return "#" + taskDto.getClassOfService().getColour();
	}

	public FocusPanel getHeader() {
		return header;
	}

	public TaskDto getDto() {
		return taskDto;
	}

	public void messageArrived(Message<TaskDto> message) {
		if (message instanceof GetSelectedTasksRequestMessage) {
            if (isSelected()) {
				MessageBus.sendMessage(new GetTasksRsponseMessage(getDto(), this));
			}
		} else if ((message instanceof TaskEditedMessage) || message instanceof TaskChangedMessage) {
			doTaskChanged(message);
		} else if (message instanceof TaskDeletedMessage) {
			if (message.getPayload().equals(getDto())) {
				unregisterListeners();	
			}
        } else if (message instanceof GetTasksByPredicateRequestMessage) {
            if (((GetTasksByPredicateRequestMessage) message).getPredicate().match(this)) {
                MessageBus.sendMessage(new GetTasksRsponseMessage(getDto(), this));
            }
        }
	}

    private void doTaskChanged(Message<TaskDto> message) {
		TaskDto payload = message.getPayload();
		if (payload.getId().equals(taskDto.getId())) {
			this.taskDto = payload;
			if (message instanceof TaskChangedMessage) {
				final String newId = ((TaskChangedMessage) message).getNewId();

				if (newId != null) {
					// this means it has been moved to a different board
					taskDto.setId(newId);

					MessageBus.unregisterListener(GetAllBoardsResponseMessage.class, boardsCollector);
					MessageBus.registerListener(GetAllBoardsResponseMessage.class, boardsCollector);
					boardsCollector.init();
					MessageBus.sendMessage(new GetBoardsRequestMessage(null, new GetBoardsRequestMessage.Filter() {
						@Override
						public boolean apply(Dtos.BoardDto boardDto) {
							return taskDto.getBoardId().equals(boardDto.getId());
						}
					}, this));

					List<Dtos.BoardDto> boards = boardsCollector.getData();
					if (boards.size() != 1) {
						// todo handle somehow this error case
					}

					boardDto = boards.iterator().next();
					taskEditingComponent.setBoardDto(boardDto);

				}
			}
			setupAccordingDto(payload);
            reevaluateFilter();
		}		
	}

	@Override
	public void activated() {
		if (!MessageBus.listens(TaskEditedMessage.class, this)) {
			MessageBus.registerListener(TaskEditedMessage.class, this);	
		}

        if (!MessageBus.listens(FilterChangedMessage.class, taskFilterChangeListener)) {
			MessageBus.registerListener(FilterChangedMessage.class, taskFilterChangeListener);
		}

		if (!MessageBus.listens(TaskChangedMessage.class, this)) {
			MessageBus.registerListener(TaskChangedMessage.class, this);	
		}
		
		if (!MessageBus.listens(TaskDeletedMessage.class, this)) {
			MessageBus.registerListener(TaskDeletedMessage.class, this);	
		}
		
		if (!MessageBus.listens(ChangeTaskSelectionMessage.class, taskSelectionChangeListener)) {
			MessageBus.registerListener(ChangeTaskSelectionMessage.class, taskSelectionChangeListener);	
		}
		
		if (!MessageBus.listens(GetSelectedTasksRequestMessage.class, this)) {
			MessageBus.registerListener(GetSelectedTasksRequestMessage.class, this);	
		}

        if (!MessageBus.listens(GetTasksByPredicateRequestMessage.class, this)) {
            MessageBus.registerListener(GetTasksByPredicateRequestMessage.class, this);
        }
	}

	@Override
	public void deactivated() {
		unregisterListeners();
	}

	private void unregisterListeners() {
		MessageBus.unregisterListener(TaskEditedMessage.class, this);
        MessageBus.unregisterListener(FilterChangedMessage.class, taskFilterChangeListener);
		MessageBus.unregisterListener(TaskChangedMessage.class, this);
		MessageBus.unregisterListener(TaskDeletedMessage.class, this);
		MessageBus.unregisterListener(ChangeTaskSelectionMessage.class, taskSelectionChangeListener);
		MessageBus.unregisterListener(GetSelectedTasksRequestMessage.class, this);
        MessageBus.unregisterListener(GetTasksByPredicateRequestMessage.class, this);
	}

    class TaskFilterChangeListener implements MessageListener<BoardsFilter> {

        @Override
        public void messageArrived(Message<BoardsFilter> message) {
            if (message.getPayload() == null) {
                return;
            }

            filter = message.getPayload();

            reevaluateFilter();
        }
    }

    public void beforeRemove(boolean partOfMove) {
        if (partOfMove) {
            // no need, the task is still present and will be re-evaluated
            return;
        }
        if (!isVisible() && filter != null) {
            // currently the only reason, but to be on the safe side adding the explicit check
            if (!filter.checkOnlyIfTaskMatches(taskDto)) {
                filter.onHiddenFieldRemoved();
            }
        }
    }

    public void reevaluateFilter() {
        boolean visible = filter == null || filter.taskMatches(taskDto, isVisible());
        setVisible(visible);
    }

    public void setFilter(BoardsFilter filter) {
        this.filter = filter;
    }

    class TaskSelectionChangeListener implements MessageListener<ChangeTaskSelectionParams> {

		@Override
		public void messageArrived(Message<ChangeTaskSelectionParams> message) {
			ChangeTaskSelectionParams params = message.getPayload();
			boolean forAll = params.isAll();
			boolean toMe = !forAll && params.getTasks().contains(getDto());
			boolean ignoreMe = !params.isApplyToYourself() && message.getSource() == TaskGui.this;
			
			if ((forAll || toMe) && !ignoreMe && isSelected() != params.isSelect()) {
				dragController.toggleSelection(TaskGui.this);
			}
		}
	}

    public FocusPanel getNamePanel() {
        return namePanel;
    }

    public FocusPanel getAssigneePicturePlace() {
        return assigneePicturePlace;
    }

    private boolean isSelected() {
        return getStyleName().contains(SELECTED_STYLE);
    }
}
